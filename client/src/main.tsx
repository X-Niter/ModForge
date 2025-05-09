import { createRoot } from "react-dom/client";
import App from "./App";
import "./index.css";

// Add Material Design Icons stylesheet
const linkElement = document.createElement("link");
linkElement.rel = "stylesheet";
linkElement.href = "https://cdn.jsdelivr.net/npm/@mdi/font@6.5.95/css/materialdesignicons.min.css";
document.head.appendChild(linkElement);

// Add page title
const titleElement = document.createElement("title");
titleElement.textContent = "MinecraftAI Mod Creator";
document.head.appendChild(titleElement);

// Add meta description for SEO
const metaDescription = document.createElement("meta");
metaDescription.name = "description";
metaDescription.content = "AI-powered Minecraft mod development system that automatically generates, tests, and iteratively improves mods based on user ideas";
document.head.appendChild(metaDescription);

// Add Open Graph tags for social sharing
const ogTitle = document.createElement("meta");
ogTitle.setAttribute("property", "og:title");
ogTitle.content = "MinecraftAI Mod Creator";
document.head.appendChild(ogTitle);

const ogDescription = document.createElement("meta");
ogDescription.setAttribute("property", "og:description");
ogDescription.content = "Generate Minecraft mods with AI, automatically test and fix errors, and continuously improve your mods";
document.head.appendChild(ogDescription);

const ogType = document.createElement("meta");
ogType.setAttribute("property", "og:type");
ogType.content = "website";
document.head.appendChild(ogType);

createRoot(document.getElementById("root")!).render(<App />);
