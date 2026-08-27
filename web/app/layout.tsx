import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Repo Archaeologist — Git history, explained",
  description: "Interroga commit, patch, blame e rinomine con un assistente locale basato su evidenze.",
  metadataBase: new URL("https://gianlucabove.it"),
  alternates: { canonical: "/repo-archaeologist/" },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="it"><body>{children}</body></html>;
}
