package main

import (
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"net/http"
	"os"
	"time"

	"github.com/joho/godotenv"
	"gopkg.in/gomail.v2"
)

type User struct {
	Email    string `json:"email"`
	Code     string `json:"code,omitempty"`
	Verified bool   `json:"verified"`
	GoogleID string `json:"google_id"`
}

var users = make(map[string]*User)

func generateCode() string {
	rand.Seed(time.Now().UnixNano())
	code := fmt.Sprintf("%06d", rand.Intn(1000000))
	return code
}

func sendVerificationEmail(email, code string) error {
	m := gomail.NewMessage()
	m.SetHeader("From", os.Getenv("EMAIL_FROM"))
	m.SetHeader("To", email)
	m.SetHeader("Subject", "Welcome to Calandy!")
	m.SetBody("text/html", fmt.Sprintf(`
        <h1>Welcome to Calandy!</h1>
        <p>Your verification code is: <strong>%s</strong></p>
        <p>Enter this code in the app to verify your account.</p>
    `, code))

	d := gomail.NewDialer(
		os.Getenv("SMTP_HOST"),
		587,
		os.Getenv("SMTP_USER"),
		os.Getenv("SMTP_PASS"),
	)

	return d.DialAndSend(m)
}

func handleGoogleSignIn(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	var user User
	if err := json.NewDecoder(r.Body).Decode(&user); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	code := generateCode()
	user.Code = code
	user.Verified = false
	users[user.Email] = &user

	if err := sendVerificationEmail(user.Email, code); err != nil {
		http.Error(w, "Failed to send verification email", http.StatusInternalServerError)
		return
	}

	response := map[string]string{"message": "Verification code sent"}
	json.NewEncoder(w).Encode(response)
}

func main() {
	if err := godotenv.Load(); err != nil {
		log.Fatal("Error loading .env file")
	}

	http.HandleFunc("/auth/google", handleGoogleSignIn)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("Server starting on port %s", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}
