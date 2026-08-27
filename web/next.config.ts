import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "export",
  basePath: "/repo-archaeologist",
  assetPrefix: "/repo-archaeologist",
  trailingSlash: true,
  images: { unoptimized: true },
};

export default nextConfig;
