<p style="text-align: center; margin-top: 40px;width: 100%;" align="center">
    <img src="./.github/blocksandstuff.png" style="width: 400px; margin: 0 auto;" />
    <p style="opacity: 90%;margin-top: -3px" align="center">Common Block & Fluid Implementations for Minestom</p>
</p>

---

<!-- TAG_REPLACEMENT -->
[![Supported Blocks](https://img.shields.io/badge/Supported_Blocks-91%25-green?style=for-the-badge)](TODO.md) [![Latest Version](https://img.shields.io/badge/Latest_Version-1.4.0--SNAPSHOT-green?style=for-the-badge)](https://mvn.everbuild.org/#/public/org/everbuild/blocksandstuff)
<!-- /TAG_REPLACEMENT -->

> This library is still in heavy development and API is subject to change in a breaking way at any time, even without a
> major release. See the [Versioning](#versioning) section for more details

This library provides a set of common implementations for blocks and fluids tailored for the Minestom framework. Its
purpose is to simplify and standardize the management of these entities in Minestom projects. By using this
library, developers can save time and focus on the unique aspects of their projects while relying on tested and reusable
components for core world functionalities.

### Key Features:

- **Blocks & Fluids**: Easy-to-use APIs for enabling block placement and block handlers as well as fluid simulation for
  both custom and default blocks and fluids
- **Efficiency**: Optimized solutions built to integrate seamlessly with the Minestom framework.
- **Flexibility**: Highly customizable implementations suitable for a variety of use cases.

See the sections below for installation instructions, versioning details, and contribution guidelines.

# Table of Contents

- [Installation & Usage](#installation--usage)
- [Versioning](#versioning)
- [Contributing](#contributing)
- [License](#license)

# Installation & Usage

> This library is not yet considered stable

The latest version is always published to our maven repository at [mvn.everbuild.org](https://mvn.everbuild.org/#/public/org/everbuild/blocksandstuff). On the page of the maven repo. you can easily check the latest version and inclusion information.

# Versioning

We're using Semantic Versioning for this project, although before the first `1.0.0`-release, we'll update API as we
see fit. In any other scenario, the versioning will follow these principles:

- **MAJOR version**: Incremented for incompatible API changes.
- **MINOR version**: Incremented for new features introduced in a backward-compatible manner.
- **PATCH version**: Incremented for backward-compatible bug fixes.
  For versions prior to `1.0.0`, the rules above are considered more flexible. Breaking changes may be introduced in
  minor versions, so it is important to keep this in mind when using pre-production versions of the library.

# Contributing

We welcome contributions to this project! If you'd like to help improve the library, follow these steps to get started:

### Steps to Contribute

1. **Fork the Repository**: Start by forking the repository to your own GitHub account.
2. **Clone the Repository**: Clone your forked repository to your local machine:
   ```bash
   git clone https://github.com/<your-username>/repository-name.git
   ```
3. **Create a Branch**: Work on a new branch specific to your feature or bug fix:
   ```bash
   git checkout -b feature-or-fix-name
   ```
4. **Write Clear Commit Messages**: Make sure your commit messages are clear and concise. We're trying to adhere to
   the [conventional commits specification](https://www.conventionalcommits.org/en/v1.0.0/)
5. **Conforming to Guidelines**: Ensure that your code follows the existing style and structure of the repository.
   Adhere to any guidelines defined in the project.
6. **Run Tests**: Before submitting a pull request, ensure your changes do not break existing functionality by running
   the tests (if applicable).
7. **Submit a Pull Request**: Push your branch to your forked repository and create a pull request against the main
   branch of this project. Please describe your intent clearly here.

### Code of Conduct

Please make sure you read and adhere to our [Code of Conduct](CODE_OF_CONDUCT.md) when contributing.

### Suggestions and Issues

If you have ideas for improvements or encounter a bug, feel free to open an issue in the issue tracker or [contact us via
discord](https://dc.asorda.net).

We appreciate your contributions, and thank you for making this library better!

# License

This project is licensed under the [MIT License](LICENSE).

This means you are free to use, modify, and distribute the library in your own projects, whether they are private or
commercial, provided that you include a copy of the MIT license.

For more detailed information, please refer to the [LICENSE](LICENSE) file in the repository.