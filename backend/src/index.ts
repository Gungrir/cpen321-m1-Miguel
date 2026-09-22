import { readFileSync } from 'fs';
import { createServer as createHttpServer } from 'http';
import { createServer as createHttpsServer } from 'https';
import { createApp } from './app';
import { env } from './config/env';
import { attachPixelRelay } from './m1';

const app = createApp();

const useHttps = env.sslKeyPath !== undefined && env.sslCertPath !== undefined;

const server = useHttps
  ? createHttpsServer(
      {
        key: readFileSync(env.sslKeyPath as string),
        cert: readFileSync(env.sslCertPath as string),
      },
      app
    )
  : createHttpServer(app);

  
attachPixelRelay(server);

server.listen(env.port, () => {
  console.log(`Server listening (${useHttps ? 'HTTPS' : 'HTTP'}) on port ${env.port}`);
});

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.on(signal, () => {
    server.close(() => {
      process.exit(0);
    });
  });
}