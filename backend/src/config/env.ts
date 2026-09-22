import 'dotenv/config';

const rawPort = process.env.PORT;
const port =
  rawPort === undefined || rawPort === ''
    ? 3000
    : Number.parseInt(rawPort, 10);

if (Number.isNaN(port) || port < 1 || port > 65535) {
  throw new Error(`Invalid PORT: ${rawPort}`);
}

function optional(name: string): string | undefined {
  const value = process.env[name];
  return value === undefined || value === '' ? undefined: value;
}

export const env = {
  port,
  sslKeyPath: optional('SSL_KEY_PATH'),
  sslCertPath: optional('SSL_CERT_PATH'),
  serverPublicIp: optional('SERVER_PUBLIC_IP') ?? 'unknown',
} as const;
