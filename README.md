# Calendy: Scan it. Save it.

## 📌 Overview
Have you ever walked past an event poster, thought it looked interesting, took a picture but then completely forgot about it? We all have, and now that image is gathering dust in our Photos app. Manually adding event details to your calendar is a hassle—and that’s where **Calendy** comes in.

With a quick scan, **Calendy** reads the poster and automatically creates a calendar entry, saving the event name, date, time, and location. No typing, no forgetting, no stress. Just **scan and go**.

### ✨ Features
- **Instant Poster Scanning** – Extracts event details from posters using OCR.
- **Seamless Calendar Integration** – Saves events directly to your preferred calendar.
- **Minimal & Intuitive UI** – No unnecessary features; just scan and save.
- **Works Offline** – Capture events even when you're not connected.
- **Smart Reminders** – Never miss an event again.

Whether you’re a student, a professional, or someone who loves discovering new events, **Calendy** keeps your plans organized with zero effort.

---

## 🚀 Deployment

### Prerequisites
Ensure you have the following installed:
- **Android Studio** (Latest version)
- **Fly.io CLI** (If deploying backend on Fly.io)
- **Python 3.8+** (For the API, if applicable)
- **Flask** (For API backend, if applicable)

### Backend Deployment (Fly.io)
1. **Install Fly.io CLI** (if not already installed):
   ```sh
   curl -L https://fly.io/install.sh | sh
   ```
2. **Authenticate & Set Up the App:**
   ```sh
   fly auth login
   fly launch --name calendy-api
   ```
3. **Deploy API**:
   ```sh
   fly deploy
   ```

---

## 🛠 Technologies Used
- **Kotlin** (for Android development)
- **Google ML Kit** (for text recognition)
- **Flask** (for backend API)
- **Fly.io** (for API deployment)

---

## 📜 License
MIT License. Feel free to use, modify, and contribute!

---

## 📬 Contact
For any questions or collaboration requests, reach out at **your@email.com** or open an issue in the repository.

**Scan it. Save it. That’s it.** 🎉

