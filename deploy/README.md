# Carland on DOKS

Shared chart: [`CarCatApp/charts`](https://github.com/CarCatApp/charts) (`carcat-app` **≥ 0.1.2** for Firebase volume).  
Values in this repo under `deploy/`.

## Safety

- **preprod only** via `deploy/install-preprod.sh` and workflow `deploy-doks-preprod.yml`
- Existing `cicd.yml` still deploys the **droplet** — unchanged
- Does not change DNS / nginx / production traffic

## Bootstrap (once)

```bash
# 1) chart tag carcat-app-0.1.2 on CarCatApp/charts (extraVolumes)
# 2) local secrets under ~/Documents/carcat_tech/.secrets/
#    - carland_preprod_env.env (pulled from droplet /root/carland.env)
#    - firebase.json (from droplet /root/secrets/firebase.json)
#    - postgres_preprod_* + redis_preprod_password.txt
chmod +x deploy/install-secret-preprod.sh deploy/install-preprod.sh
./deploy/install-secret-preprod.sh
./deploy/install-preprod.sh
```

Expect CrashLoop until **carlanddatabase** schema/data is migrated (same situation as auth).

Repo secrets for GitHub Actions (`deploy-doks-preprod.yml`):

| Secret | Purpose |
|--------|---------|
| `DOCKER_USERNAME` / `DOCKER_PASSWORD` | already used by cicd.yml |
| `KUBE_CONFIG` | base64 DOKS kubeconfig |
| `CHARTS_READ_TOKEN` | PAT if private charts + GITHUB_TOKEN can't read |

GitHub Environment **`preprod`** (optional protection rules).
