import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyCW7E3XoGEotbHwHMdTQhPq1k9L5I9-rwI",
  authDomain: "ai-based-women-safety-app.firebaseapp.com",
  projectId: "ai-based-women-safety-app",
  storageBucket: "ai-based-women-safety-app.firebasestorage.app",
  messagingSenderId: "492250547241",
  appId: "1:492250547241:web:17e631cf677533fe264838"
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);