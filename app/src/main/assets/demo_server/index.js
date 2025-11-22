const http = require('http');

const PORT = process.env.PORT || 8080;

const server = http.createServer((req, res) => {
  res.writeHead(200, { 'Content-Type': 'text/html' });
  res.end(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>Droidploy Demo Server</title>
      <style>
        body {
          font-family: system-ui, -apple-system, sans-serif;
          max-width: 800px;
          margin: 50px auto;
          padding: 20px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
        }
        .container {
          background: rgba(255, 255, 255, 0.1);
          backdrop-filter: blur(10px);
          padding: 40px;
          border-radius: 20px;
          box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
        }
        h1 { margin: 0 0 10px 0; font-size: 2.5em; }
        .status {
          background: #10b981;
          padding: 10px 20px;
          border-radius: 25px;
          display: inline-block;
          margin: 20px 0;
          font-weight: bold;
        }
        .info {
          background: rgba(0, 0, 0, 0.2);
          padding: 20px;
          border-radius: 10px;
          margin: 20px 0;
        }
        code {
          background: rgba(0, 0, 0, 0.3);
          padding: 2px 8px;
          border-radius: 4px;
          font-family: 'Courier New', monospace;
        }
      </style>
    </head>
    <body>
      <div class="container">
        <h1>🚀 Droidploy is Live!</h1>
        <div class="status">✓ Server Running</div>
        <div class="info">
          <p><strong>🎉 Congratulations!</strong></p>
          <p>Your Node.js backend is running on Android and accessible via Cloudflare Tunnel!</p>
          <p><strong>Request Info:</strong></p>
          <ul>
            <li>Method: <code>${req.method}</code></li>
            <li>URL: <code>${req.url}</code></li>
            <li>User-Agent: <code>${req.headers['user-agent']}</code></li>
          </ul>
          <p><strong>Server:</strong> Node.js ${process.version}</p>
          <p><strong>Platform:</strong> ${process.platform} (${process.arch})</p>
        </div>
        <p style="opacity: 0.8; margin-top: 30px;">
          🔧 Edit your <code>index.js</code> to customize this server!
        </p>
      </div>
    </body>
    </html>
  `);
});

server.listen(PORT, () => {
  console.log(`✓ Server listening on port ${PORT}`);
  console.log(`✓ Node.js ${process.version}`);
  console.log(`✓ Platform: ${process.platform} (${process.arch})`);
});

server.on('error', (err) => {
  console.error('Server error:', err);
  process.exit(1);
});

