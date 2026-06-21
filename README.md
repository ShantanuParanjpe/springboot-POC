# springboot-CD

# ArgoCD Deployment on AKS and Monitor on Grafana with Slack Notification

## Project Overview

This project demonstrates a production-grade GitOps workflow for deploying and observing a Spring Boot application on Azure Kubernetes Service (AKS). Application deployments are managed declaratively through ArgoCD, ensuring continuous synchronization between the Git repository and the cluster state. Operational visibility is provided via Grafana dashboards, with proactive alerting configured to deliver real-time Slack notifications upon pod or node failures, enabling rapid incident response.

---

## GitHub Repo Setup

1. Create ArgoCD application folder
2. Create ArgoCD springboot application in this folder with name as `argocd-springboot-app.yml`
3. Create `k8s-manifest` folder and create the `springboot-deployment.yml` and `service.yml` in it

---

## ArgoCD Installation

### Login to AKS Cluster

Add the ArgoCD helm repo using the below command:

```bash
helm repo add argo https://argoproj.github.io/argo-helm
```

> "argo" has been added to your repositories

### Create a Namespace for ArgoCD

```bash
kubectl create namespace argocd
```

### Install the ArgoCD Helm Chart

```bash
helm install argocd argo/argo-cd --namespace argocd --version 9.5.21
```

### Expose ArgoCD Server via LoadBalancer

The ArgoCD service would be running as a ClusterIP. Change the service type to `LoadBalancer` using the below command:

```bash
kubectl patch svc argocd-server -n argocd \
  -p '{"spec": {"type": "LoadBalancer"}}'
```

### Deploy the ArgoCD Application

Create the ArgoCD application by applying the yml configuration using the below command:

```bash
kubectl apply -f argocd-springboot-app.yml
```

### Fetch ArgoCD UI Credentials

```bash
kubectl get secret argocd-initial-admin-secret \
  -n argocd \
  -o jsonpath="{.data.password}" | base64 -d && echo
```

### Verify the Deployment

1. Login to the ArgoCD UI through the Azure LoadBalancer IP and confirm the `springboot-app` is visible.
2. If visible, commit the `springboot-deployment.yml` and `springboot-app-service.yml` to GitHub.
3. Verify that ArgoCD is able to sync the changes and create both objects successfully.

---

## Grafana Monitoring & Slack Alerts

Once the application is deployed, Grafana is used to monitor the springboot application pods. Alerts are configured to send notifications to a Slack channel whenever:

- A **pod** goes down
- A **node** goes down

---

## Grafana and Prometheus Stack Installation

### 1. Install Prometheus + Grafana

Add the Prometheus community Helm repository:

```bash
helm repo add prometheus-community \
  https://prometheus-community.github.io/helm-charts
helm repo update
```

Install the `kube-prometheus-stack` Helm chart:

```bash
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace
```

This installs the following components:

| Component | Purpose |
|---|---|
| **Prometheus** | Metrics collection and storage |
| **Grafana** | Metrics visualisation and dashboards |
| **kube-state-metrics** | Tracks pod health automatically |
| **node-exporter** | Tracks node health |

### 2. Expose Grafana

```bash
kubectl patch svc prometheus-grafana \
  -n monitoring \
  -p '{"spec": {"type": "LoadBalancer"}}'

# Wait for external IP to be assigned
kubectl get svc prometheus-grafana -n monitoring -w
```

### 3. Get Grafana Password

```bash
kubectl get secret prometheus-grafana \
  -n monitoring \
  -o jsonpath="{.data.admin-password}" | base64 -d && echo
```

### 4. Access Grafana Locally via Port-Forward

To access the Grafana UI, expose the service locally using the following command:

```bash
kubectl port-forward svc/prometheus-grafana 3000:80 -n monitoring
```

### 5. Access Grafana UI

Navigate to `http://localhost:3000` in your browser and log in using the credentials retrieved in Step 3.

### 6. Import a Pod Health Dashboard

After logging in, create a dashboard by importing an existing Pod Health dashboard:

1. Click **Dashboards** → **Import**
2. Enter the dashboard ID (e.g., a Kubernetes pod health dashboard from grafana.com)
3. Select **Prometheus** as the data source
4. Click **Save**

---

## Setup Slack Alerts via Grafana

### Step 1: Generate a Slack Webhook URL

1. Create an account on Slack using your personal email address.
2. Create a workspace.
3. Within your workspace, create a dedicated channel (e.g., `#aks-alerts`).
4. Navigate to [api.slack.com/apps](https://api.slack.com/apps) and click **Create New App** → **From Scratch**. Name your app `Grafana Alerter` and select your workspace.
5. Under the features list, click **Incoming Webhooks** and toggle the switch to **Activate**.
6. Click **Add New Webhook to Workspace**, select your `#aks-alerts` channel, and click **Allow**.
7. Copy the generated Webhook URL. It will follow the format:
   ```
   https://hooks.slack.com/services/T000/B000/XXXXXX
   ```

### Step 2: Create the Slack Contact Point in Grafana

1. In the Grafana sidebar, navigate to **Alerting** → **Contact points**.
2. Click **+ Add contact point**.
3. Fill in the form as follows:
   - **Name:** `Slack-Alerts`
   - **Integration:** `Slack`
   - **Webhook URL:** Paste the URL copied from Step 1.
4. Click **Test**, select **Custom notice**, enter a test message, and verify the notification appears in your Slack channel.
5. Click **Save contact point**.

### Step 3: Create the "Pod Down" Alert Rule

1. In the Grafana sidebar, navigate to **Alerting** → **Alert rules** and click **+ Create alert rule**.

2. **Section 1 — Rule name:** `Kubernetes Pod Stuck / Down`

3. **Section 2 — Define query:**
   - Select your **Prometheus** data source.
   - Enter the following PromQL query to track waiting/failing pods:
     ```promql
     sum(kube_pod_container_status_waiting_reason{reason=~"ErrImagePull|ImagePullBackOff|CrashLoopBackOff"}) by (namespace, pod)
     ```

4. **Section 3 — Condition:** Set the threshold to trigger when the value is **above 0** (i.e., at least one pod is stuck).

5. **Section 4 — Evaluation behavior:**
   - **Evaluate every:** `2m`

6. **Section 5 — Configure notifications:** Under **Notification Policy**, ensure the alert labels route to the `Slack-Alerts` contact point created in Step 2.

7. Click **Save rule and exit**.
