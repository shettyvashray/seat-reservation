CREATE TABLE shows (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_paise BIGINT NOT NULL,
    per_user_limit INTEGER NOT NULL DEFAULT 4,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE seats (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reservation_id UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_seat_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT uq_show_seat
        UNIQUE (show_id, seat_number)
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    show_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    amount_paise BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_reservation_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id),

    CONSTRAINT uq_reservation_idempotency
        UNIQUE (show_id, user_id, idempotency_key)
);

CREATE TABLE show_user_locks (
    show_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,

    CONSTRAINT pk_show_user_locks
        PRIMARY KEY (show_id, user_id),

    CONSTRAINT fk_show_user_lock_show
        FOREIGN KEY (show_id)
        REFERENCES shows(id)
);

CREATE INDEX idx_seats_show_status
    ON seats(show_id, status);

CREATE INDEX idx_reservations_show_user
    ON reservations(show_id, user_id);