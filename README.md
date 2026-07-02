# eBPMNv2

A discrete-event simulation engine for BPMN-based process models, developed at the Systems Engineering Lab, Università di Roma Tor Vergata.

**Authors:** Paolo Bocciarelli

---

## Prerequisites

- Java 21 or later
- Apache Maven 3.8 or later
- Pitch pRTI Student Edition 5.5.10 (required for distributed simulation via HLA)

---

## Step 1 — Obtain Pitch pRTI Student Edition

The Pitch pRTI jars are required for compilation. They are **not included** in this repository and **cannot be redistributed**: each user must register individually to obtain their own copy.

Register and download Pitch pRTI Student Edition 5.5.10 at:
https://pitchtechnologies.com/prti/

> **Note:** local (non-distributed) simulation does not require an active HLA federation, but the Pitch jars must still be present on your machine for the project to compile.

---

## Step 2 — Register Pitch jars in your local Maven repository

Pitch pRTI does not need to be installed — simply extract the downloaded archive to any directory on your machine. Then register the three jars in your local Maven repository by running the following commands from a terminal, replacing `/path/to/prti/lib/` with the actual path to the extracted Pitch lib directory:

```bash
mvn install:install-file \
    -Dfile=/path/to/prti/lib/pRTI1516e.jar \
    -DgroupId=com.pitch \
    -DartifactId=pRTI1516e \
    -Dversion=5.5.10 \
    -Dpackaging=jar

mvn install:install-file \
    -Dfile=/path/to/prti/lib/pRTIcore.jar \
    -DgroupId=com.pitch \
    -DartifactId=pRTIcore \
    -Dversion=5.5.10 \
    -Dpackaging=jar

mvn install:install-file \
    -Dfile=/path/to/prti/lib/booster1516.jar \
    -DgroupId=com.pitch \
    -DartifactId=booster1516 \
    -Dversion=5.5.10 \
    -Dpackaging=jar
```

You only need to do this once per machine.

---

## Step 3 — Clone and install eBPMNv2

```bash
git clone https://github.com/uniroma2-sel/eBPMN.git
cd eBPMN
mvn install
```

This installs the eBPMNv2 jar in your local Maven repository (`~/.m2`).

---

## Verify

To verify that the Pitch jars are correctly registered:

```bash
ls ~/.m2/repository/com/pitch/
```

You should see three directories: `pRTI1516e`, `pRTIcore`, `booster1516`.

---

## Repository

https://github.com/uniroma2-sel/eBPMN
