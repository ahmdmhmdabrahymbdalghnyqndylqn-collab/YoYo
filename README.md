# YoYo
Private Android chat + audio calling prototype for up to six trusted users.

## Zero-cost relay mode
This build uses a private, high-entropy ntfy topic as a lightweight relay and encrypts app payloads with AES-GCM. It is intended for a small trusted group/prototype. For production-grade account verification, guaranteed background delivery and TURN fallback, replace the relay module with a dedicated backend.

## Build
`gradle :app:assembleDebug`
