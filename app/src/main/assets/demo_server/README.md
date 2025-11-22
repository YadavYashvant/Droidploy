# Droidploy Demo Server

This is a simple Node.js HTTP server that demonstrates running a backend on Android via Droidploy.

## What it does

- Serves a beautiful landing page showing the server is live
- Displays request information (method, URL, headers)
- Shows Node.js version and platform details

## How to customize

1. Edit `index.js` to add your own routes and logic
2. You can use any Node.js built-in modules (http, fs, path, etc.)
3. For external packages, you'll need to include `node_modules` in your project folder

## Example: Add a JSON API endpoint

```javascript
const server = http.createServer((req, res) => {
  if (req.url === '/api/status') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ 
      status: 'ok', 
      timestamp: Date.now(),
      uptime: process.uptime()
    }));
  } else {
    // ... existing code
  }
});
```

## Port Configuration

The server automatically uses the `PORT` environment variable (set to 8080 by Droidploy) or falls back to 8080 if not set.

