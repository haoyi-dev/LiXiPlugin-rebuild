import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";

export default defineConfig({
  site: "https://simpmc-studio.github.io",
  base: "/LiXiPlugin",
  integrations: [
    starlight({
      title: "LiXiPlugin",
      description:
        "Hệ thống phát lì xì cho server Minecraft Paper/Folia",
      logo: {
        src: "./src/assets/red_envelope.png",
      },
      favicon: "/favicon.ico",
      social: {
        github: "https://github.com/SimpMC-Studio/LiXiPlugin",
      },

      defaultLocale: "root",
      locales: {
        root: {
          label: "Tiếng Việt",
          lang: "vi",
        },
      },
      customCss: ["./src/styles/custom.css"],
      sidebar: [
        {
          label: "Giới thiệu",
          items: [
            {
              slug: "getting-started",
              label: "Bắt đầu",
            },
          ],
        },
        {
          label: "Cấu hình",
          items: [
            {
              slug: "configuration/main-config",
              label: "Cấu hình chính",
            },
          ],
        },
        {
          label: "Lệnh",
          items: [
            {
              slug: "commands/user-commands",
              label: "Lệnh người chơi",
            },
            {
              slug: "commands/admin-commands",
              label: "Lệnh quản trị",
            },
          ],
        },
        {
          label: "Hướng dẫn",
          items: [
            {
              slug: "guides/set-item",
              label: "Thiết lập vật phẩm",
            },
            {
              slug: "guides/permissions",
              label: "Quyền hạn",
            },
          ],
        },
      ],
    }),
  ],
});
