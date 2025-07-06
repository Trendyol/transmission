# GitHub Pages Documentation Setup Guide

This guide explains how to set up automatic documentation deployment to GitHub Pages using GitHub Actions, integrating both MkDocs and Dokka API documentation.

## 🚀 Quick Setup

### 1. Repository Settings

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Pages**
3. Under **Source**, select **GitHub Actions**
4. Save the settings

### 2. Documentation Structure

The documentation system combines:
- **MkDocs**: Main documentation site with guides, tutorials, and examples
- **Dokka**: API documentation automatically generated from code comments
- **Integration**: API docs are served under `/api/0.x/` path within the MkDocs site

### 3. Workflow Files

We've provided two workflow options:

#### Option A: Simple Workflow (`.github/workflows/docs.yml`)
- ✅ Generates MkDocs site with integrated API docs
- ✅ Builds Dokka API documentation
- ✅ Deploys combined site to GitHub Pages
- ✅ Simple and reliable

#### Option B: Advanced Workflow (`.github/workflows/docs-advanced.yml`)
- ✅ All features from simple workflow
- ✅ Version information in documentation
- ✅ PR preview artifacts
- ✅ Tag-based releases
- ✅ Automatic PR comments

**Choose one workflow** - delete the other to avoid conflicts.

### 4. Repository Permissions

Ensure your repository has the correct permissions:

1. Go to **Settings** → **Actions** → **General**
2. Under **Workflow permissions**, select:
   - ✅ **Read and write permissions**
   - ✅ **Allow GitHub Actions to create and approve pull requests**

## 📖 Documentation URLs

After setup, your documentation will be available at:

```
https://<username>.github.io/<repository-name>/
```

API documentation will be available at:
```
https://<username>.github.io/<repository-name>/api/0.x/
```

For the Transmission library:
```
Main site: https://trendyol.github.io/transmission/
API docs:  https://trendyol.github.io/transmission/api/0.x/
```

## 🔧 Local Development

### Building Documentation Locally

To build the complete documentation site locally:

```bash
# Install MkDocs and dependencies
pip install mkdocs-material pymdown-extensions

# Generate API documentation
./gradlew generateApiDocs

# Build the complete site
mkdocs build

# Serve locally for development
mkdocs serve
```

The combined site will be available at `http://localhost:8000` with API docs at `http://localhost:8000/api/0.x/`.

### Project Structure

```
docs/                          # MkDocs documentation
├── index.md                  # Main documentation
├── api-overview.md           # API overview (included in Dokka)
└── ...
build/site/                   # Generated documentation site
├── index.html               # MkDocs main page
├── api/0.x/                 # Dokka API documentation
│   ├── index.html           # API documentation entry point
│   └── ...
└── ...
```

## 🔧 Workflow Features

### Simple Workflow Features
- **Automatic Generation**: Runs on every push to main/master
- **Manual Trigger**: Can be triggered manually from Actions tab
- **MkDocs + Dokka Integration**: Builds both documentation types
- **Gradle Caching**: Faster builds with Gradle caching
- **Error Handling**: Detailed error reporting with stacktraces

### Advanced Workflow Features
- **Version Display**: Shows version info in documentation
- **PR Previews**: Generates documentation artifacts for pull requests
- **Release Tagging**: Special handling for version tags
- **PR Comments**: Automatic comments on PRs with download links
- **Build Metadata**: Commit hash and build timestamp

## 🏷️ Version Management

### API Version Configuration

The API documentation is versioned using the `apiVersion` variable in `build.gradle.kts`:

```kotlin
val apiVersion = "0.x"  // Update this for different versions
```

### Release Versions
When you create a tag (e.g., `v1.0.0`):
```bash
git tag v1.0.0
git push origin v1.0.0
```
The documentation will show version information and API docs will be available under the appropriate version path.

## 🛠️ Customization

### Modifying Documentation Generation

To customize the documentation generation, edit the tasks in `build.gradle.kts`:

```kotlin
tasks.register("generateApiDocs") {
    group = "documentation"
    description = "Generates API documentation for all Transmission modules"
    dependsOn("dokkaHtmlMultiModule")
}
```

### Adding Custom Pages

You can add custom markdown files to be included in the documentation:

1. Create `.md` files in the `docs/` directory
2. Add them to the `nav` section in `mkdocs.yml`
3. They'll be included in the next build

### Navigation Customization

The navigation is configured in `mkdocs.yml`. The API link is automatically included:

```yaml
nav:
  - 'Introduction': index.md
  - 'How To Use': 'how_to_use.md'
  # ... other pages
  - 'API': 'api/0.x/'  # Points to Dokka API docs
```

## 🔍 Troubleshooting

### Common Issues

1. **API documentation not found**
   - Ensure `generateApiDocs` task completes successfully
   - Check that Dokka is properly configured for all modules
   - Verify the API version path matches in navigation

2. **MkDocs build failures**
   - Check that all referenced markdown files exist
   - Verify navigation structure in `mkdocs.yml`
   - Ensure required Python packages are installed

3. **404 Page Not Found**
   - Ensure GitHub Pages is enabled
   - Check that the workflow completed successfully
   - Wait a few minutes for deployment

### Debug Commands

Run locally to test:
```bash
# Generate API documentation only
./gradlew generateApiDocs

# Build complete site
mkdocs build

# Serve locally
mkdocs serve

# Check generated structure
find build/site -name "*.html" | head -10
```

## 📊 Monitoring

### GitHub Actions Dashboard

Monitor your documentation builds:
1. Go to **Actions** tab in your repository
2. Click on **Generate and Deploy Documentation**
3. View build logs and deployment status

### GitHub Pages Status

Check deployment status:
1. Go to **Settings** → **Pages**
2. View deployment history
3. Check for any errors or warnings

## 🔄 Maintenance

### Regular Updates

Keep your workflows updated:
1. Update action versions quarterly
2. Monitor for deprecated features
3. Test workflows with new Gradle/Java versions
4. Update MkDocs and Python dependencies

### Security Considerations

- Workflows use minimal required permissions
- No secrets are exposed in documentation
- All actions use official, maintained versions

## 📚 Additional Resources

- [GitHub Pages Documentation](https://docs.github.com/en/pages)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Dokka Documentation](https://kotlinlang.org/docs/dokka-introduction.html)
- [MkDocs Documentation](https://www.mkdocs.org/)
- [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/)
- [Gradle Build Action](https://github.com/gradle/gradle-build-action) 