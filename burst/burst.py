import asyncio
import os
import sys
from dataclasses import dataclass
from typing import Any

import httpx


DEFAULT_ADMIN_TOKEN = "dev-admin-token"
REQUEST_TIMEOUT = 30.0

HOT_SEAT_REQUESTS = 20
IDEMPOTENCY_REQUESTS = 20
PER_USER_REQUESTS = 10
EXPECTED_PER_USER_LIMIT = 4


@dataclass
class TestResult:
    name: str
    passed: bool
    message: str


class BurstTester:

    def __init__(self, base_url: str, admin_token: str):
        self.base_url = base_url.rstrip("/")
        self.admin_token = admin_token

        self.client = httpx.AsyncClient(
            base_url=self.base_url,
            timeout=REQUEST_TIMEOUT,
        )

        self.results: list[TestResult] = []

    async def close(self):
        await self.client.aclose()

    def record(
        self,
        name: str,
        passed: bool,
        message: str,
    ):
        self.results.append(
            TestResult(
                name=name,
                passed=passed,
                message=message,
            )
        )

        status = "PASS" if passed else "FAIL"
        print(f"[{status}] {name}: {message}")

    async def create_show(
        self,
        name: str,
        seats: list[str],
    ) -> dict[str, Any]:

        response = await self.client.post(
            "/shows",
            headers={
                "Authorization": f"Bearer {self.admin_token}",
                "Content-Type": "application/json",
            },
            json={
                "name": name,
                "seats": seats,
                "price_paise": 10000,
            },
        )

        if response.status_code != 201:
            raise RuntimeError(
                f"Failed to create show: "
                f"{response.status_code} {response.text}"
            )

        return response.json()

    async def get_show(
        self,
        show_id: str,
        user_id: str = "burst-reader",
    ) -> dict[str, Any]:

        response = await self.client.get(
            f"/shows/{show_id}",
            headers={
                "Authorization": f"Bearer {user_id}",
            },
        )

        if response.status_code != 200:
            raise RuntimeError(
                f"Failed to get show: "
                f"{response.status_code} {response.text}"
            )

        return response.json()

    async def reserve(
        self,
        show_id: str,
        user_id: str,
        seats: list[str],
        idempotency_key: str,
    ) -> httpx.Response:

        return await self.client.post(
            f"/shows/{show_id}/reserve",
            headers={
                "Authorization": f"Bearer {user_id}",
                "Idempotency-Key": idempotency_key,
                "Content-Type": "application/json",
            },
            json={
                "seats": seats,
            },
        )

    async def cancel(
        self,
        reservation_id: str,
        user_id: str,
    ) -> httpx.Response:

        return await self.client.post(
            f"/shows/reservations/{reservation_id}/cancel",
            headers={
                "Authorization": f"Bearer {user_id}",
            },
        )

    async def run_hot_seat_storm(self):

        print("\n=== HOT-SEAT STORM ===")

        seats = [f"A{i}" for i in range(1, 6)]

        show = await self.create_show(
            "Burst - Hot Seat",
            seats,
        )

        show_id = show["id"]

        tasks = [
            self.reserve(
                show_id=show_id,
                user_id=f"hot-user-{i}",
                seats=["A1"],
                idempotency_key=f"hot-seat-{i}",
            )
            for i in range(HOT_SEAT_REQUESTS)
        ]

        responses = await asyncio.gather(
            *tasks,
            return_exceptions=True,
        )

        status_codes = [
            response.status_code
            for response in responses
            if isinstance(response, httpx.Response)
        ]

        exceptions = [
            response
            for response in responses
            if isinstance(response, Exception)
        ]

        successful = status_codes.count(201)
        conflicts = status_codes.count(409)
        server_errors = [
            status
            for status in status_codes
            if status >= 500
        ]

        state = await self.get_show(show_id)

        passed = (
            len(exceptions) == 0
            and successful == 1
            and conflicts == HOT_SEAT_REQUESTS - 1
            and len(server_errors) == 0
            and state["confirmed"] == 1
            and state["available"] == len(seats) - 1
            and (
                state["available"]
                + state["held"]
                + state["confirmed"]
                == state["total_seats"]
            )
        )

        self.record(
            "Hot-seat storm",
            passed,
            (
                f"201={successful}, "
                f"409={conflicts}, "
                f"5xx={len(server_errors)}, "
                f"exceptions={len(exceptions)}, "
                f"confirmed={state['confirmed']}"
            ),
        )

        return passed

    async def run_idempotency_storm(self):

        print("\n=== IDEMPOTENCY STORM ===")

        seats = ["A1", "A2", "A3"]

        show = await self.create_show(
            "Burst - Idempotency",
            seats,
        )

        show_id = show["id"]

        tasks = [
            self.reserve(
                show_id=show_id,
                user_id="idempotent-user",
                seats=["A1"],
                idempotency_key="same-key-001",
            )
            for _ in range(IDEMPOTENCY_REQUESTS)
        ]

        responses = await asyncio.gather(
            *tasks,
            return_exceptions=True,
        )

        http_responses = [
            response
            for response in responses
            if isinstance(response, httpx.Response)
        ]

        exceptions = [
            response
            for response in responses
            if isinstance(response, Exception)
        ]

        successful = [
            response
            for response in http_responses
            if response.status_code == 201
        ]

        server_errors = [
            response
            for response in http_responses
            if response.status_code >= 500
        ]

        reservation_ids = set()

        for response in successful:
            body = response.json()
            reservation_ids.add(body["reservation_id"])

        state = await self.get_show(show_id)

        passed = (
            len(exceptions) == 0
            and len(successful) == IDEMPOTENCY_REQUESTS
            and len(server_errors) == 0
            and len(reservation_ids) == 1
            and state["confirmed"] == 1
            and state["available"] == len(seats) - 1
        )

        self.record(
            "Same-key idempotency storm",
            passed,
            (
                f"responses={len(http_responses)}, "
                f"201={len(successful)}, "
                f"unique_reservation_ids={len(reservation_ids)}, "
                f"5xx={len(server_errors)}, "
                f"confirmed={state['confirmed']}"
            ),
        )

        return passed

    async def run_idempotency_conflict(self):

        print("\n=== IDEMPOTENCY CONFLICT ===")

        show = await self.create_show(
            "Burst - Idempotency Conflict",
            ["A1", "A2"],
        )

        show_id = show["id"]

        first = await self.reserve(
            show_id=show_id,
            user_id="idempotency-conflict-user",
            seats=["A1"],
            idempotency_key="conflict-key",
        )

        second = await self.reserve(
            show_id=show_id,
            user_id="idempotency-conflict-user",
            seats=["A2"],
            idempotency_key="conflict-key",
        )

        state = await self.get_show(show_id)

        passed = (
            first.status_code == 201
            and second.status_code == 409
            and state["confirmed"] == 1
            and state["available"] == 1
        )

        self.record(
            "Same-key different-body conflict",
            passed,
            (
                f"first={first.status_code}, "
                f"second={second.status_code}, "
                f"confirmed={state['confirmed']}, "
                f"available={state['available']}"
            ),
        )

        return passed

    async def run_per_user_limit_storm(self):

        print("\n=== PER-USER LIMIT STORM ===")

        seats = [f"A{i}" for i in range(1, PER_USER_REQUESTS + 1)]

        show = await self.create_show(
            "Burst - Per User Limit",
            seats,
        )

        show_id = show["id"]

        tasks = [
            self.reserve(
                show_id=show_id,
                user_id="limit-user",
                seats=[seat],
                idempotency_key=f"limit-key-{i}",
            )
            for i, seat in enumerate(seats)
        ]

        responses = await asyncio.gather(
            *tasks,
            return_exceptions=True,
        )

        http_responses = [
            response
            for response in responses
            if isinstance(response, httpx.Response)
        ]

        exceptions = [
            response
            for response in responses
            if isinstance(response, Exception)
        ]

        successful = [
            response
            for response in http_responses
            if response.status_code == 201
        ]

        conflicts = [
            response
            for response in http_responses
            if response.status_code == 409
        ]

        server_errors = [
            response
            for response in http_responses
            if response.status_code >= 500
        ]

        state = await self.get_show(show_id)

        passed = (
            len(exceptions) == 0
            and len(successful) == EXPECTED_PER_USER_LIMIT
            and len(conflicts) == (PER_USER_REQUESTS - EXPECTED_PER_USER_LIMIT)
            and len(server_errors) == 0
            and state["confirmed"] == EXPECTED_PER_USER_LIMIT
            and state["available"] == (PER_USER_REQUESTS - EXPECTED_PER_USER_LIMIT)
            and (
                state["available"]
                + state["held"]
                + state["confirmed"]
                == state["total_seats"]
            )
        )

        self.record(
            "Per-user concurrency limit",
            passed,
            (
                f"201={len(successful)}, "
                f"409={len(conflicts)}, "
                f"5xx={len(server_errors)}, "
                f"exceptions={len(exceptions)}, "
                f"confirmed={state['confirmed']}"
            ),
        )

        return passed

    async def run_cancel_and_rebook(self):

        print("\n=== CANCEL + REBOOK ===")

        show = await self.create_show(
            "Burst - Cancel Rebook",
            ["A1", "A2"],
        )

        show_id = show["id"]

        reservation_response = await self.reserve(
            show_id=show_id,
            user_id="cancel-user",
            seats=["A1"],
            idempotency_key="cancel-key",
        )

        if reservation_response.status_code != 201:
            self.record(
                "Cancel + rebook",
                False,
                (
                    "Initial reservation failed: "
                    f"{reservation_response.status_code}"
                ),
            )
            return False

        reservation_id = reservation_response.json()[
            "reservation_id"
        ]

        cancel_response = await self.cancel(
            reservation_id=reservation_id,
            user_id="cancel-user",
        )

        rebook_response = await self.reserve(
            show_id=show_id,
            user_id="rebook-user",
            seats=["A1"],
            idempotency_key="rebook-key",
        )

        state = await self.get_show(show_id)

        passed = (
            cancel_response.status_code == 200
            and rebook_response.status_code == 201
            and state["confirmed"] == 1
            and state["available"] == 1
            and (
                state["available"]
                + state["held"]
                + state["confirmed"]
                == state["total_seats"]
            )
        )

        self.record(
            "Cancel + rebook",
            passed,
            (
                f"cancel={cancel_response.status_code}, "
                f"rebook={rebook_response.status_code}, "
                f"confirmed={state['confirmed']}, "
                f"available={state['available']}"
            ),
        )

        return passed

    async def run_reconciliation(self):

        print("\n=== RECONCILIATION ===")

        show = await self.create_show(
            "Burst - Reconciliation",
            ["A1", "A2", "A3", "A4", "A5"],
        )

        show_id = show["id"]

        state = await self.get_show(show_id)

        calculated_total = (
            state["available"]
            + state["held"]
            + state["confirmed"]
        )

        passed = (
            calculated_total == state["total_seats"]
        )

        self.record(
            "State reconciliation",
            passed,
            (
                f"available={state['available']}, "
                f"held={state['held']}, "
                f"confirmed={state['confirmed']}, "
                f"total={state['total_seats']}"
            ),
        )

        return passed

    async def run_all(self):

        print("======================================")
        print(" Seat Reservation - Burst Test")
        print("======================================")
        print(f"Base URL: {self.base_url}")

        await self.run_hot_seat_storm()
        await self.run_idempotency_storm()
        await self.run_idempotency_conflict()
        await self.run_per_user_limit_storm()
        await self.run_cancel_and_rebook()
        await self.run_reconciliation()

        print("\n======================================")
        print(" Summary")
        print("======================================")

        passed = 0
        failed = 0

        for result in self.results:
            status = "PASS" if result.passed else "FAIL"

            print(
                f"{status:4} | "
                f"{result.name} | "
                f"{result.message}"
            )

            if result.passed:
                passed += 1
            else:
                failed += 1

        print("--------------------------------------")
        print(
            f"Passed: {passed} | "
            f"Failed: {failed}"
        )

        if failed > 0:
            return 1

        print("\nALL BURST TESTS PASSED")
        return 0


async def main():

    if len(sys.argv) != 2:
        print(
            "Usage: ./burst.sh <BASE_URL>",
            file=sys.stderr,
        )
        sys.exit(2)

    base_url = sys.argv[1]

    admin_token = os.getenv(
        "ADMIN_TOKEN",
        DEFAULT_ADMIN_TOKEN,
    )

    tester = BurstTester(
        base_url=base_url,
        admin_token=admin_token,
    )

    try:
        exit_code = await tester.run_all()
    finally:
        await tester.close()

    sys.exit(exit_code)


if __name__ == "__main__":
    asyncio.run(main())