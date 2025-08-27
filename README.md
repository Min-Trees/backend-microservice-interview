# abc-interview-support-backend

This project is a microservice-based backend. The `auth-service` handles authentication
and delegates all user data operations to the separate `user-service` via a Feign client.
Only the `user-service` owns the `users` table and related persistence logic.
