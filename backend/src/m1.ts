import { Router } from 'express';
import type { Server as HttpServer } from 'http';
import type { Server as HttpsServer } from 'https';
import WebSocket, { WebSocketServer } from 'ws';
import { env } from './config/env';

const PIXEL_SERVER_URL = 'wss://8.229.22.124';

export function formatTime(date: Date = new Date()): string {
  const pad = (n: number): string => String(n).padStart(2, '0');
  const offset = -date.getTimezoneOffset(); 
  const sign = offset >= 0 ? '+' : '-';
  const abs = Math.abs(offset);
  return (
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} ` +
    `GMT${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`
  );
}

export const m1Router = Router();

m1Router.get('/server-ip', (_req, res) => {
  res.json({ ip: env.serverPublicIp });
});

m1Router.get('/server-time', (_req, res) => {
  res.json({ time: formatTime() });
});

m1Router.get('/name', (_req, res) => {
  res.json({ firstName: 'Miguel', lastName: 'Tampubolon' });
});

m1Router.get('/client-ip', (req, res) => {
  const ip = (req.ip ?? req.socket.remoteAddress ?? 'unknown').replace('::ffff:', '');
  res.json({ ip });
});

/**
 * Relays the course pixel stream to the app at wss://<server-ip>/pixels.
 * Each app connection gets its own upstream connection, and every message is
 * forwarded immediately and unchanged (no batching, delaying, or reformatting).
 */
export function attachPixelRelay(server: HttpServer | HttpsServer): void {
  const wss = new WebSocketServer({ server, path: '/pixels' });

  wss.on('connection', (client) => {
    // The course server likely uses a self-signed certificate
    const upstream = new WebSocket(PIXEL_SERVER_URL, { rejectUnauthorized: false });

    upstream.on('message', (data, isBinary) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data, { binary: isBinary });
      }
    });
    upstream.on('error', (err) => {
      console.error('Pixel upstream error:', err.message);
    });
    upstream.on('close', () => {
      if (client.readyState === WebSocket.OPEN) client.close();
    });

    client.on('close', () => upstream.terminate());
    client.on('error', () => upstream.terminate());
  });
}