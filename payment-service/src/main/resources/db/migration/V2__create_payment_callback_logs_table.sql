CREATE TABLE IF NOT EXISTS payment_callback_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID REFERENCES payment_transactions(id) ON DELETE SET NULL,
    raw_payload TEXT NOT NULL,
    signature_valid BOOLEAN NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_callback_logs_transaction_id ON payment_callback_logs(transaction_id);
CREATE INDEX idx_payment_callback_logs_received_at ON payment_callback_logs(received_at);
