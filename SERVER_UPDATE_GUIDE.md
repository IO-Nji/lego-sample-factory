# Server Update Scripts

## Quick Update (Recommended for Server)

Use `update-from-registry.sh` on your server for quick updates:

```bash
# On your server (io-surf)
cd ~/lego-sample-factory
./update-from-registry.sh
```

**What it does:**
1. ✅ Checks registry connectivity
2. ✅ Pulls all 8 service images (latest tags)
3. ✅ Stops running services
4. ✅ Starts services with new images
5. ✅ Shows service status
6. ✅ Reports any failed services

**Simple, automated, no manual confirmations needed.**

---

## Alternative: Manual Pull with Confirmation

Use `pull-from-registry.sh` if you want manual control:

```bash
./pull-from-registry.sh
```

This script asks for confirmation before restarting services.

---

## Transfer Scripts to Server

```bash
# From development machine
scp update-from-registry.sh nji@io-surf:~/lego-sample-factory/
scp pull-from-registry.sh nji@io-surf:~/lego-sample-factory/

# SSH to server and make executable
ssh nji@io-surf
cd ~/lego-sample-factory
chmod +x update-from-registry.sh pull-from-registry.sh
```

---

## Troubleshooting

**Registry not accessible:**
```bash
# Check registry is running on Ubuntu server
ssh <registry-server>
docker ps | grep registry
curl http://192.168.1.237:5000/v2/_catalog
```

**Services fail to start:**
```bash
# Check logs
docker-compose logs -f <service-name>

# Restart specific service
docker-compose restart <service-name>

# Full restart
docker-compose down && docker-compose up -d
```

**Out of disk space:**
```bash
# Clean old images
docker system prune -a

# Check disk usage
df -h
docker system df
```

---

## Registry Info

- **Address**: 192.168.1.237:5000
- **Protocol**: HTTP (insecure registry)
- **Project**: lego-sample-factory
- **Services**: 8 microservices + frontend + nginx proxy

**List available images:**
```bash
curl http://192.168.1.237:5000/v2/_catalog | python3 -m json.tool
```

**Check image tags:**
```bash
curl http://192.168.1.237:5000/v2/lego-sample-factory-frontend/tags/list | python3 -m json.tool
```
