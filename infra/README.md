# Deploy AiHelper to AWS from zero (AWS Console, click by click) - screenintegration.com

The result: one EC2 instance in **Europe (Frankfurt) eu-central-1**, with nginx routing:

```
https://DOMAIN/            -> React UI
https://DOMAIN/aihelper/*  -> Spring server (REST + websocket)
```

`infra/aihelper-stack.yml` builds all of it (network, firewall, server, fixed IP, DNS, HTTPS, Java, nginx).
You upload it once in CloudFormation; after that GitHub Actions deploys the app.

Rough cost: ~ $20/month (t3.small + Elastic IP) + domain $3-15/year.

---

## Step 1 - AWS account and region

1. Go to https://aws.amazon.com → **Create an AWS Account** (or sign in as the root/admin user).
2. Top-right corner, region dropdown → choose **Europe (Frankfurt) eu-central-1**.
   Every step below (except Route 53, which is global) must be done in this region.
3. Recommended - a cost alarm:
   **Billing and Cost Management** → **Budgets** → **Create budget** → *Use a template* →
   **Monthly cost budget** → amount `30` USD → your email → **Create budget**.

## Step 2 - Buy the domain (Route 53)

1. Search bar at the top → type **Route 53** → open it.
2. Left menu → **Domains → Registered domains** → **Register domains**.
3. Type the name you want → **Search** → on an available one click **Select** → **Proceed to checkout**.
4. Duration 1 year, **Auto-renew** on → **Next**.
5. Fill in contact info, keep **Privacy protection** on → **Next** → tick the terms → **Submit**.
6. Open the email from AWS and click the verification link (the domain is suspended after 15 days otherwise).
7. Wait until **Registered domains** shows it (minutes to a few hours; **Requests** shows progress).

Route 53 creates a **hosted zone** for the domain automatically - you'll just pick it from a list in step 3.

> This guide is filled in for **screenintegration.com**: UI at `https://screenintegration.com/`, server at `https://screenintegration.com/aihelper`.

### If Route 53 says "We can't finish registering your domain"

Usually a new / Free-plan account or an unverified card. Check **Billing → Payment preferences** (card verified),
**Account** (upgrade from Free plan), and open the support case from the error (billing cases are free).

Or buy the domain at another registrar (Porkbun, Namecheap, Cloudflare) and keep DNS in Route 53:

1. Buy the domain there.
2. **Route 53 → Hosted zones → Create hosted zone** → domain name → **Public hosted zone** → **Create** ($0.50/month).
3. Open the zone → copy the 4 values of the **NS** record (`ns-123.awsdns-45.com` ...).
4. At the registrar: domain → **Nameservers → Custom** → paste the 4 values (no trailing dot) → save.
5. Wait until it switches (usually < 1 hour, up to 48h) - then continue with step 3 and pick this zone.

## Step 3 - Create everything with CloudFormation (~5 min)

1. Search bar → **CloudFormation** → check the region is still **Frankfurt**.
2. **Create stack** → **With new resources (standard)**.
3. *Prepare template*: **Choose an existing template**. *Template source*: **Upload a template file** →
   **Choose file** → select `AiHelperServer/infra/aihelper-stack.yml` → **Next**.
4. **Stack name**: `aihelper`. Parameters:

   | Parameter | Value |
   |---|---|
   | `DomainName` | your domain, e.g. `screenintegration.com` |
   | `HostedZoneId` | pick your domain from the dropdown |
   | `CertificateEmail` | your email (Let's Encrypt expiry notices) |
   | `BasePath` | leave `/aihelper` |
   | `InstanceType` | leave `t3.small` |
   | `SshCidr`, `AmiId` | leave defaults |

   → **Next**.
5. *Configure stack options*: change nothing. At the bottom tick
   **"I acknowledge that AWS CloudFormation might create IAM resources."** → **Next**.
6. Review → **Submit**.
7. Watch the **Events** tab until the stack status is **CREATE_COMPLETE** (~3-5 min).
   If it shows `ROLLBACK_COMPLETE`, the first red `CREATE_FAILED` event says why - fix it, delete the stack, repeat.
8. Open the **Outputs** tab and keep it open. You need `DeployHost`, `ApiBaseUrl` and `SiteUrl`.

HTTPS is set up by itself as soon as the domain points at the server (normally a few minutes;
it keeps retrying for 2 hours). Until then the site works on plain `http://`.

## Step 4 - Copy the deploy key

1. Search bar → **Systems Manager** → left menu **Application Tools → Parameter Store**.
2. Click the parameter named `/ec2/keypair/key-...` (created by the stack).
3. In **Value** click **Show** (or the copy icon) - copy **all** of it, including the
   `-----BEGIN OPENSSH PRIVATE KEY-----` and `-----END ...-----` lines.

Keep it private - anyone with it can log in to the server.

## Step 5 - GitHub secrets and variables

In each repo on github.com: **Settings → Secrets and variables → Actions**.
*Secrets* tab → **New repository secret**; *Variables* tab → **New repository variable**.

**`Barchenko1/AiHelperServer`**

| Tab | Name | Value |
|---|---|---|
| Secrets | `DEPLOY_HOST` | `DeployHost` output (your domain) |
| Secrets | `DEPLOY_KEY` | the key from step 4 |
| Secrets | `APP_JWT_SECRET` | any random text, 32+ characters |
| Secrets | `APP_NOOP_SECRET` | password suffix for the users in `SecurityConfig` |
| Secrets | `GPT_API_KEY` | OpenAI key |
| Secrets | `ANTHROPIC_API_KEY` | Anthropic key |
| Variables | `APP_BASE_PATH` | `/aihelper` |

**`Barchenko1/AiHelperUI`**

| Tab | Name | Value |
|---|---|---|
| Secrets | `DEPLOY_HOST` | `DeployHost` output (your domain) |
| Secrets | `DEPLOY_KEY` | same key from step 4 |
| Variables | `REACT_APP_AI_API` | `ApiBaseUrl` output, `https://screenintegration.com/aihelper` |

If a secret with that name already exists (from the old server), click it → **Update secret**.

## Step 6 - Deploy

1. **AiHelperServer** repo → **Actions** tab → **CI/CD Spring Boot → EC2** → **Run workflow** → branch `master` → **Run workflow**.
   Wait for the green check (tests + coverage gate run first).
2. **AiHelperUI** repo → **Actions** → **Build & Deploy React to EC2** → **Run workflow** → **Run workflow**.

From now on, every push to `master` deploys automatically.

## Step 7 - Check it

1. Open `https://screenintegration.com/aihelper/api/v1/status` in the browser → should show `{"session":false, ...}`.
2. Open the `SiteUrl` output → log in → the **Session** light turns green.

**If something doesn't work**, open a terminal on the server from the browser:
**EC2** → **Instances** → select `aihelper` → **Connect** → tab **Session Manager** → **Connect**. Then:

| What | Command |
|---|---|
| Setup script log | `sudo tail -100 /var/log/cloud-init-output.log` |
| Server log | `journalctl -u AiHelperServer -n 200 --no-pager` |
| nginx errors | `sudo tail /var/log/nginx/error.log` |
| HTTPS certificate progress | `journalctl -u aihelper-https --no-pager` |

## Step 8 - Point the other clients at it

- **CLI**: `aiHelperServerUrl=https://screenintegration.com/aihelper` in `src/main/resources/application.properties`.
- **Extension**: open the popup → type `https://screenintegration.com/aihelper` in **URL** → **Save**.

## Later

- **Old server** (`16.170.163.130`, region Stockholm): switch the region to **Europe (Stockholm)** →
  **EC2 → Instances** → select it → **Instance state → Terminate**. If it had an Elastic IP:
  **EC2 → Elastic IPs** → select → **Actions → Release**.
- **Remove the new setup**: **CloudFormation** → select `aihelper` → **Delete**. The domain stays yours.
- **Change the prefix**: the setup script only runs on the first boot, so changing `BasePath` on the existing
  stack does not touch nginx. Instead connect via Session Manager, edit `location /aihelper/` in
  `/etc/nginx/conf.d/aihelper.conf`, run `sudo nginx -t && sudo systemctl reload nginx`; then update
  `APP_BASE_PATH` and `REACT_APP_AI_API` in GitHub and rerun both workflows.
