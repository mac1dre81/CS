# TecMarketa — CredStow Landing Page (GitHub Pages)

This directory (`docs/`) hosts the **GitHub Pages** site for **TecMarketa**'s flagship
Android app, **CredStow** — a secure AI API key manager.

## What's Here

| File | Purpose |
|------|---------|
| `index.html` | Main landing page with SEO, structured data (JSON-LD), and Google Ads optimization |
| `style.css` | Stylesheet using the app's Material 3 color palette (blue-600 / teal-700 / violet-600) |
| `robots.txt` | Controls crawler access for Google Search & Google Ads |
| `sitemap.xml` | Helps Google discover and index all pages on the site |
| `privacy_policy.html` | CredStow privacy policy (linked from the app and Google Play Store) |
| `data_deletion.html` | Data deletion instructions for Play Store / GDPR compliance |
| `images/` | SVG images: logo, favicon, hero mockup, app screenshots |
| `.nojekyll` | Disables Jekyll processing so all files are served as-is |

## Enabling GitHub Pages

1. Go to your repository on GitHub: **Settings → Pages**
2. Under **Source**, select:
   - Branch: `main` (or `master`)
   - Folder: `/docs`
3. Click **Save**. GitHub will build and deploy your site in ~1–2 minutes.
4. Your site will be available at: `https://mac1dre81.github.io/CS/`

> **Tip:** To use a custom domain (e.g. `tecmarketa.com`), add a `CNAME` file
> in this directory containing just the domain name, then configure your DNS
> with a `CNAME` record pointing to `mac1dre81.github.io`.

## Google Search Console & Google Ads

- **Verify ownership** in [Google Search Console](https://search.google.com) by
  adding a `google-site-verification` meta tag to `index.html` (a placeholder is
  already in place).
- **Google Ads conversion tracking**: Uncomment the `gtag.js` snippet in the
  `<head>` of `index.html` and replace `AW-XXXXXXXXX` with your Google Ads
  conversion ID. The example click-tracking snippet (commented at the bottom)
  tracks Play Store link clicks as conversions.

## Images

All images are **SVG** (scalable vector graphics) for crisp rendering on any
device and native search-engine crawlability (text is selectable, alt text is
provided). Mockups reflect the actual app's Material 3 UI.

## License

CredStow is developed by **TecMarketa**. All rights reserved.
