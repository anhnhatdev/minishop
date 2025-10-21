CREATE TABLE IF NOT EXISTS notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    channel VARCHAR(20) NOT NULL,
    subject VARCHAR(255),
    body_template TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_templates_code ON notification_templates(code);

-- Seed initial default notification templates
INSERT INTO notification_templates (id, code, channel, subject, body_template)
VALUES
(
    gen_random_uuid(),
    'ORDER_CONFIRMED',
    'EMAIL',
    'Xác nhận đơn hàng #{{orderCode}} — MiniShop',
    'Xin chào {{customerName}},<br/><br/>Đơn hàng <strong>#{{orderCode}}</strong> của bạn trị giá <strong>{{totalAmount}} VND</strong> đã được thanh toán và xác nhận thành công.<br/>Chúng tôi đang chuẩn bị hàng và sẽ sớm giao đến địa chỉ: <em>{{shippingAddress}}</em>.<br/><br/>Cảm ơn bạn đã tin tưởng mua sắm tại MiniShop!'
),
(
    gen_random_uuid(),
    'ORDER_CANCELLED',
    'EMAIL',
    'Thông báo hủy đơn hàng #{{orderCode}} — MiniShop',
    'Xin chào {{customerName}},<br/><br/>Đơn hàng <strong>#{{orderCode}}</strong> của bạn đã bị hủy.<br/>Lý do: <em>{{cancelReason}}</em>.<br/>Nếu đơn hàng đã được trừ tiền, số tiền sẽ được hoàn trả tự động theo quy định.<br/><br/>Trân trọng cảm ơn!'
),
(
    gen_random_uuid(),
    'PAYMENT_FAILED',
    'EMAIL',
    'Thanh toán đơn hàng #{{orderCode}} không thành công — MiniShop',
    'Xin chào {{customerName}},<br/><br/>Giao dịch thanh toán cho đơn hàng <strong>#{{orderCode}}</strong> (số tiền: {{totalAmount}} VND) đã không thành công.<br/>Lý do: {{failureReason}}.<br/>Vui lòng thử lại phương thức thanh toán khác hoặc chọn hình thức COD.<br/><br/>MiniShop Support Team.'
),
(
    gen_random_uuid(),
    'WELCOME_EMAIL',
    'EMAIL',
    'Chào mừng bạn đến với cộng đồng MiniShop!',
    'Xin chào {{userName}},<br/><br/>Chúc mừng bạn đã đăng ký tài khoản thành công tại <strong>MiniShop</strong> với email: <em>{{userEmail}}</em>.<br/>Hãy khám phá hàng ngàn sản phẩm chất lượng cao với ưu đãi hấp dẫn ngay hôm nay!<br/><br/>Thân ái,<br/>Đội ngũ MiniShop.'
),
(
    gen_random_uuid(),
    'ORDER_SHIPPING',
    'EMAIL',
    'Đơn hàng #{{orderCode}} đang trên đường giao đến bạn — MiniShop',
    'Xin chào {{customerName}},<br/><br/>Đơn hàng <strong>#{{orderCode}}</strong> của bạn đã được xuất kho và đang trên đường vận chuyển.<br/>Mã vận đơn: <strong>{{trackingNumber}}</strong>.<br/>Địa chỉ nhận hàng: <em>{{shippingAddress}}</em>.<br/><br/>Chúc bạn một ngày vui vẻ!'
),
(
    gen_random_uuid(),
    'ORDER_DELIVERED',
    'EMAIL',
    'Đơn hàng #{{orderCode}} đã được giao thành công — MiniShop',
    'Xin chào {{customerName}},<br/><br/>Đơn hàng <strong>#{{orderCode}}</strong> đã được giao thành công đến bạn.<br/>Hãy dành chút thời gian đánh giá sản phẩm để nhận các voucher ưu đãi cho đơn hàng tiếp theo nhé!<br/><br/>Cảm ơn bạn đã đồng hành cùng MiniShop.'
);
