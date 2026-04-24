import { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import { getMatches } from "./api/matchApi";
import type { Match } from "./types/Match";
import { HomePage } from "./pages/HomePage";
import "../styles.css";

function App() {
  const [matches, setMatches] = useState<Match[]>([]);

  useEffect(() => {
    getMatches().then(setMatches).catch(console.error);
  }, []);

  return <HomePage matches={matches} />;
}

createRoot(document.getElementById("root")!).render(<App />);
