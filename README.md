

1. **User:**
    - ID (Auto-generated)
    - Username
    - Password
    - Email
    - Phone Number
    - Address
    - Role (Customer or Restaurant Owner)
    - Registration Date
    - Last Login Date
2. **Restaurant:**
    - ID (Auto-generated)
    - Name
    - Description
    - Cuisine Type
    - Address
    - Contact Information
    - Opening Hours
    - Ratings
    - Image URL
    - Registration Date
3. **Menu Item:**
    - ID (Auto-generated)
    - Name
    - Description
    - Price
    - Category
    - Image URL
    - Availability Status
    - Restaurant (reference to Restaurant entity)
    - Creation Date
4. **Order:**
    - ID (Auto-generated)
    - Customer (reference to User entity)
    - Restaurant (reference to Restaurant entity)
    - Total Amount
    - Order Status
    - Timestamp
    - Delivery Address
    - Items (list of Order Items)
    - Payment (reference to Payment entity, if applicable)
5. **Order Item:**
    - ID (Auto-generated)
    - Menu Item (reference to Menu Item entity)
    - Quantity
    - Subtotal
    - Order (reference to Order entity)
6. **Payment:**
    - ID (Auto-generated)
    - Order (reference to Order entity)
    - Payment Method
    - Payment Status
    - Total Amount
    - Payment Timestamp
7. **~~Delivery Executive:~~**
    - ~~ID (Auto-generated)~~
    - ~~Name~~
    - ~~Contact Information~~
    - ~~Availability Status~~
    - ~~Current Location (Latitude and Longitude)~~
8. **Review/Rating:**
    - ID (Auto-generated)
    - Customer (reference to User entity)
    - Restaurant (reference to Restaurant entity)
    - Rating
    - Review Text
    - Timestamp
9. **Promotion/Coupon:**
    - ID (Auto-generated)
    - Code
    - Discount Amount
    - Validity Period
    - Terms and Conditions
10. **Notification:**
    - ID (Auto-generated)
    - Recipient (reference to User, Restaurant, or Delivery Executive entity)
    - Message
    - Timestamp
    - Read Status
11. **Category:**
    - ID (Auto-generated)
    - Name
12. **Address:**
    - ID (Auto-generated)
    - Street Address
    - City
    - State/Province
    - Postal Code
    - Country
    
13. contact information
    - email
    - mobile
    - twitter
    - instagram

**service**

**service-implementation**

**controller**

Product images uploaded from the administrator app are stored in
`./uploads/product-images` and served publicly at `/uploads/product-images/**`.
Set `APP_UPLOADS_PATH` to a persistent directory when deploying the backend so
uploaded images survive application/container restarts.

## Runtime configuration

Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET` before starting the API.

Set `APP_PASSWORD_RESET_URL` to the React password reset page in the same environment
and configure the SMTP connection before using password recovery. The sender defaults
to `SMTP_USERNAME`. Set `APP_MAIL_FROM` if it uses a different address.
Use a randomly generated JWT secret of at least 32 bytes. Replacing the previous
signing key requires users to sign in again. Public registration creates a
customer account. Provision restaurant owners and platform administrators through
an authorized administration process.

## Stripe checkout configuration

Configure these environment variables before starting the backend:

| Variable | Purpose |
| --- | --- |
| `STRIPE_SECRET_KEY` | Stripe secret API key for creating and reconciling Checkout sessions. |
| `STRIPE_WEBHOOK_SECRET` | Signing secret for validating Stripe webhook requests. |
| `STRIPE_CHECKOUT_BACKEND_URL` | Public base URL of this backend, used for Stripe's success and cancel return URLs. |
| `STRIPE_CHECKOUT_RETURN_URL` | Client URL to which the backend redirects after reconciling a Checkout return. |

For the React client, point the return URL to its `/payment/return` route.
The Android client refreshes the authenticated order when the user returns from
the browser. Use a backend URL reachable from the device and persist
`APP_UPLOADS_PATH` on the backend that serves the catalog.

Register `POST /api/payments/stripe/webhook` in Stripe and subscribe to:
`checkout.session.completed`, `checkout.session.expired`,
`checkout.session.async_payment_failed`, and
`checkout.session.async_payment_succeeded`. The webhook must be reachable by
Stripe and its signing secret must match `STRIPE_WEBHOOK_SECRET`. Do not treat
the Checkout redirect as proof of payment; the backend validates the signed
webhook (and reconciles status when queried) before exposing an order to the
restaurant.

The authenticated client can query
`GET /api/order/{orderId}/payment-status` to recover the current order/payment
state after returning to the app or reopening it. The Checkout return endpoint
is `GET /api/payments/stripe/return`; it redirects to the configured client URL
with `order_id` and `payment_status` query parameters.

Run the backend integration tests with `mvnw.cmd test`. The webhook tests use a
local test signing secret and cover successful, cancelled, failed, and
unconfirmed payments, invalid signatures, and duplicate event delivery. They
do not make a live Stripe transaction; end-to-end payment verification requires
Stripe test credentials and a publicly reachable webhook endpoint.

## Restaurant Rewards

Rewards are stored in the same Spring Boot persistence database as users and
orders. With the current `spring.jpa.hibernate.ddl-auto=update` configuration,
the backend creates/updates the rewards tables at startup; no Supabase storage
or client-side balance is used.

Authenticated endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/rewards` | Membership state, point balance, and active catalog. |
| `POST` | `/api/rewards/join` | Idempotently enroll the current user. |
| `GET` | `/api/rewards/catalog` | Active reward catalog. |
| `GET` | `/api/rewards/history` | Latest point movements for the current user. |

The initial catalog contains 100 points for a COP 5,000 discount on orders of
at least COP 25,000, and 200 points for a COP 12,000 discount on orders of at
least COP 50,000. The earning rate is one point per COP 1,000 actually paid,
rounded down. Only enrolled customers earn points, and only after Stripe
confirms payment. A reward redemption reserves points before checkout; failed
or expired payments restore them. Unique ledger references and database locks
protect credits, reservations, and refunds from duplicate requests/events.

Catalog seed amounts are configurable with `REWARDS_STARTER_POINTS`,
`REWARDS_STARTER_DISCOUNT_COP`, `REWARDS_STARTER_MINIMUM_ORDER_COP`,
`REWARDS_PLUS_POINTS`, `REWARDS_PLUS_DISCOUNT_COP`, and
`REWARDS_PLUS_MINIMUM_ORDER_COP`. Changes are synchronized to the seeded
catalog on backend startup; the active flag remains stored in the database.
The order total and selected reward are recalculated and validated by the
backend before creating the Stripe Checkout session.

`mvnw.cmd test` includes membership/catalog persistence, server-calculated
checkout discounts, post-payment point accrual, and duplicate/failure webhook
coverage. It uses the project test database and does not perform a live Stripe
charge.

## Notifications

The backend stores notifications for the recipient user. A paid order creates a
`NEW_ORDER` notification for the restaurant owner; order status changes create
`ORDER_STATUS_*` notifications for the customer. Each signed-in account reads
and marks only its own notifications through:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/notifications` | List the signed-in user's notifications. |
| `PUT` | `/api/notifications/{id}/read` | Mark one owned notification as read. |
| `PUT` | `/api/notifications/read-all` | Mark all owned notifications as read. |
| `POST` | `/api/notifications/device-token` | Register or transfer this device's FCM token. |
| `DELETE` | `/api/notifications/device-token?token=...` | Unregister this account's device token. |

Hibernate updates the notification and device-token tables when the backend
starts (`spring.jpa.hibernate.ddl-auto=update`). To enable remote delivery,
create a Firebase service account for the same Firebase project used by the
mobile app and provide its file path to the backend using
`GOOGLE_APPLICATION_CREDENTIALS`. Keep the service-account JSON outside the
repository and restrict access to it. Without this setting, the backend keeps
notifications in the inbox and logs that push delivery is disabled.

Push delivery runs after the notification transaction commits, so an FCM error
does not roll back a payment or order update. Failed FCM sends are logged; the
notification remains available in the inbox, but automatic durable push retries
are not implemented.

Register Android and iOS apps in Firebase. Configure an APNs authentication key
in Firebase for iOS push delivery. Build/run Flutter with the Firebase project
and app identifiers documented in `restaurantapp/README.md`. The app requests
notification permission after sign-in, registers the device token against the
current account, refreshes the inbox while signed in, and unregisters the token
on sign-out. Firebase credentials are never sent to or stored in Flutter.
