CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL UNIQUE,
    transaction_code VARCHAR(50) NOT NULL UNIQUE,
    gateway_transaction_id VARCHAR(100),
    amount NUMERIC(12, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    gateway_response_code VARCHAR(20),
    payment_url VARCHAR(1000),
    paid_at TIMESTAMP WITH TIME ZONE,
    expired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_transactions_order_id ON payment_transactions(order_id);
CREATE INDEX idx_payment_transactions_transaction_code ON payment_transactions(transaction_code);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
