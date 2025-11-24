# Droidploy 🚀📱

> Turn your Android phone into a mini-server. Because why not? 

## What is this?

Droidploy lets you run actual Node.js backends **on your Android phone** and deploy them to the internet with zero hassle. No root required, no sketchy workarounds—just pure Android magic.

## Why would I want this?

Good question! Here are some legit use cases:

- **Quick prototyping** - Test your backend ideas without touching AWS
- **Personal APIs** - Run your own micro-services from your phone
- **IoT hub** - Turn your phone into a home automation server
- **Learning** - Understand how servers work by deploying on hardware you already own
- **Flex on your friends** - "Yeah, my phone is hosting my website" 😎

## Features

✅ **Node.js on Android** - Full Node.js runtime running natively  
✅ **Cloudflare Tunnels** - Automatic HTTPS and public URL generation  
✅ **Zero Configuration** - Import your project, click deploy, done  
✅ **No Root Required** - Works on any Android 10+ device  
✅ **Background Execution** - Keeps running even when you lock your phone  
✅ **Custom Domains** - Use your own domain via Cloudflare  

## How it works

Droidploy does some pretty cool engineering under the hood:

1. **Native Libraries Hack** - We package Node.js and Cloudflared as native libraries (`.so` files) to bypass Android's W^X security restrictions
2. **Cloudflare API Automation** - Automatically creates tunnels, configures DNS, and routes traffic
3. **Foreground Service** - Keeps your server alive in the background
4. **QUIC Protocol** - Uses Cloudflare's QUIC protocol to establish tunnels (because Android's DNS can be wonky)

The magic? We leverage Android's `nativeLibraryDir` to execute binaries that would normally be blocked. Check out `ServerService.kt` if you want to see the sorcery.

## Requirements

- Android 10+ (API Level 29+)
- A Cloudflare account (free tier works!)
- A domain managed by Cloudflare
- ~100MB free storage

## Setup

### 1. Get Cloudflare Credentials

You'll need these from your Cloudflare dashboard:

- **API Token** - Go to [dash.cloudflare.com](https://dash.cloudflare.com) → My Profile → API Tokens → Create Token
  - Use "Edit Cloudflare Tunnel" template or create custom with these permissions:
    - Account → Cloudflare Tunnel → Edit
    - Zone → DNS → Edit
- **Account ID** - Found on any domain's overview page (right sidebar)
- **Zone ID** - On your domain's overview page (right sidebar)
- **Domain** - Your actual domain (e.g., `example.com`)

### 2. Configure the App

1. Open Droidploy
2. Tap the Settings icon (⚙️)
3. Enter your Cloudflare credentials
4. Hit "SAVE CONFIGURATION"

### 3. Deploy Your Server

**Option 1: Use the demo server**
- Just hit "DEPLOY SERVER" to see it in action
- It'll deploy a sample Node.js server

**Option 2: Deploy your own code**
1. Zip your Node.js project (make sure `index.js` is at the root or adjust the command)
2. Tap "IMPORT PROJECT (ZIP/FOLDER)"
3. Select your zip file
4. (Optional) Customize the command (default: `node index.js`)
5. Hit "DEPLOY SERVER"
6. Wait ~10 seconds for magic to happen
7. Click the URL to open your live site! 🎉

## Project Structure

Your Node.js project should look like:

```
my-project.zip
├── index.js         # Entry point
├── package.json     # Dependencies (optional)
└── ... other files
```

**Important:** The app extracts your zip and runs the command from the root. Make sure your entry file matches the command!

## Troubleshooting

### "Cannot find module" error
- Make sure your `index.js` is at the root of the zip
- Check that the custom command matches your file structure

### Tunnel fails with DNS errors
- The app uses hardcoded Cloudflare edge IPs to bypass DNS issues
- If it still fails, check your internet connection

### Server stops after a while
- Android is aggressive about killing background processes
- Keep the app in "Recent Apps" or whitelist it in battery settings

### libnode.so not found
- This means the native binaries aren't in `jniLibs/arm64-v8a/`
- Make sure `useLegacyPackaging = true` is set in `build.gradle.kts`

## Architecture Deep Dive

For the nerds (like me):

### The W^X Problem
Android 10+ blocks execution of binaries from writable directories. We bypass this by:
1. Packaging executables as `.so` files in `jniLibs/arm64-v8a/`
2. Android extracts them to `/data/app/.../lib/arm64/` (read-only, executable)
3. We launch them via `ProcessBuilder` as if they were shared libraries

### Binary Compatibility
- Node.js needs to be **statically linked** or built for Android's Bionic libc
- Standard Linux ARM64 binaries won't work (they use glibc)
- We use a custom Android-compatible Node.js build

### Cloudflare Integration
- Creates tunnel via REST API (not CLI)
- Configures ingress rules programmatically
- Creates DNS CNAME automatically
- Uses QUIC protocol with hardcoded edge IPs (DNS workaround)

## Tech Stack

- **Kotlin** - Android app
- **Jetpack Compose** - UI
- **Node.js** (statically linked) - Runtime
- **Cloudflared** - Tunnel daemon
- **Cloudflare API** - Tunnel/DNS management
- **Retrofit** - HTTP client

## Building from Source

```bash
# Clone the repo
git clone https://github.com/yourusername/droidploy.git
cd droidploy

# Add binaries to jniLibs
# Place these in app/src/main/jniLibs/arm64-v8a/:
# - libnode.so (Android-compatible Node.js binary)
# - libcloudflared.so (Cloudflared ARM64 binary)

# Build
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Where to get binaries?

- **Node.js**: Use [nodejs-android-prebuilt-binaries](https://github.com/sjitech/nodejs-android-prebuilt-binaries) or build from source
- **Cloudflared**: Grab from [Cloudflare releases](https://github.com/cloudflare/cloudflared/releases) (use linux-arm64, rename to `.so`)

## Known Limitations

- ❌ No npm install at runtime (pre-bundle dependencies)
- ❌ ARM64 devices only (no x86 support yet)
- ❌ Single server per app instance
- ❌ Updates require re-importing the project
- ⚠️ Battery drain if running 24/7 (it's still a phone!)

## Contributing

PRs welcome! Some ideas:

- [ ] Support for Deno/Bun runtimes
- [ ] Built-in code editor
- [ ] Python/Go server support
- [ ] Auto-restart on crashes
- [ ] Server monitoring dashboard
- [ ] Multiple tunnel support