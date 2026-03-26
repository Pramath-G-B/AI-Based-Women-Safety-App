# AI-Based-Women-Safety-App --- SafeGuard
##  Problem Statement
Women safety and emergency response in India is still reactive rather than proactive.  
Many safety apps exist, but they require manual triggering and are not widely used.

---

##  Solution
SafeGuard is an AI-powered background safety system that continuously monitors environmental signals like location and activity to detect potential danger situations automatically.

It runs silently in the background and triggers alerts when high-risk situations are detected.

---

##  Features
-  Real-time location tracking (even in background)
-  Persistent safety monitoring using foreground service
-  Fast detection loop (runs every few seconds)
-  Firebase integration for alert storage
-  Modular AI pipeline (Sensor → Risk → Alert)
-  Works without user interaction after activation

---

##  How It Works

1. App runs a **foreground service**
2. Continuously collects:
   - Location data
   - (Future: audio + motion)
3. Converts data into `SensorInput`
4. (Upcoming) Risk Engine analyzes danger level
5. If high risk → alert is triggered and stored in Firebase

---

##  Tech Stack

- **Android (Kotlin)**
- **Firebase Firestore**
- **Google Location Services**
- **Foreground Services (Android 12+)**

---
