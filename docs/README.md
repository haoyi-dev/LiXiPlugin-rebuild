# LiXiPlugin Documentation

This directory contains the Starlight documentation site for LiXiPlugin.

## 🚀 Quick Start

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## 📁 Structure

```
docs/
├── public/              # Static assets
│   └── favicon.svg
├── src/
│   ├── content/
│   │   └── docs/        # Documentation pages (MDX)
│   │       ├── index.mdx
│   │       ├── getting-started.mdx
│   │       ├── configuration/
│   │       ├── commands/
│   │       ├── guides/
│   │       └── vi/      # Vietnamese translations
│   └── styles/
│       └── custom.css   # Theme customization
├── astro.config.mjs     # Astro & Starlight config
└── package.json
```

## 🌍 Internationalization

The site supports both English (default) and Vietnamese:

- **English**: `/` (root)
- **Vietnamese**: `/vi/`

Language switcher is available in the header.

## 🎨 Theme

The site uses a custom red & gold theme matching the Vietnamese lucky money aesthetic. See `src/styles/custom.css` for color definitions.

## 📝 Adding Content

### English Page

Create a new `.mdx` file in `src/content/docs/`:

```mdx
---
title: My Page Title
description: Page description for SEO
---

## Content here

Your content using Markdown and MDX.
```

### Vietnamese Translation

Create the same file structure under `src/content/docs/vi/`:

```mdx
---
title: Tiêu đề trang
description: Mô tả trang cho SEO
---

## Nội dung ở đây

Nội dung của bạn sử dụng Markdown và MDX.
```

## 🔧 Configuration

Edit `astro.config.mjs` to:
- Add/remove sidebar items
- Change theme colors
- Modify social links
- Adjust i18n settings

## 📖 Resources

- [Starlight Documentation](https://starlight.astro.build/)
- [Astro Documentation](https://docs.astro.build/)
- [MiniMessage Format](https://docs.advntr.dev/minimessage/format.html)

## 📜 License

Same as LiXiPlugin main project.
