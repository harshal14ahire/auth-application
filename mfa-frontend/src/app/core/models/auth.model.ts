export interface LoginRequest { username: string; password: string; }
export interface RegisterRequest { username: string; email: string; password: string; phoneNumber?: string; }
export interface MfaVerifyRequest { mfaToken: string; code: string; method: string; }
export interface AuthResponse { token: string; tokenType: string; mfaRequired: boolean; mfaToken: string; mfaMethods: string[]; message: string; }
export interface ApiResponse { success: boolean; message: string; data: any; }
export interface UserProfile { id: string; username: string; email: string; phoneNumber: string; provider: string; mfaEnabled: boolean; mfaMethods: string[]; }
export interface TotpSetup { secret: string; qrCodeUri: string; message: string; }
