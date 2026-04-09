<p style="text-align: center; margin-top: 40px;width: 100%;" align="center">
    <img src="./.github/blocksandstuff.png" style="width: 400px; margin: 0 auto;" />
    <p style="opacity: 90%;margin-top: -3px" align="center">Common Block & Fluid Implementations for Minestom</p>
</p>

---

<!-- TAG_REPLACEMENT -->
[![Supported Blocks](https://img.shields.io/badge/Supported_Blocks-100%25-green?style=for-the-badge)](TODO.md) [![Latest Version](https://img.shields.io/badge/Latest_Version-1.9.1--SNAPSHOT-green?style=for-the-badge)](https://mvn.everbuild.org/#/public/org/everbuild/blocksandstuff)
<!-- /TAG_REPLACEMENT -->

This library provides a set of common implementations for blocks and fluids tailored for the Minestom framework. Its
purpose is to simplify and standardize the management of these entities in Minestom projects. By using this
library, developers can save time and focus on the unique aspects of their projects while relying on tested and reusable
components for core world functionalities.

### Key Features:

- **Blocks, Fluids & Recipes**: Easy-to-use APIs for enabling block placement, handlers, recipes, and fluid simulation for vanilla or custom blocks.
- **Efficiency**: Optimized solutions built to integrate seamlessly with the Minestom framework.
- **Flexibility**: Highly customizable implementations allowing developers to selectively include only the features they need.

See the sections below for installation instructions, versioning details, and contribution guidelines.

# Table of Contents

- [Installation & Usage](#installation--usage)
- [Versioning](#versioning)
- [Contributing](#contributing)
- [License](#license)

# Installation & Usage

Blocks-and-Stuff is split into four modules:
- `blocksandstuff-common` contains common APIs for blocksandstuff and your own code to communicate
- `blocksandstuff-blocks` contains block implementations, both placement and interaction
- `blocksandstuff-fluids` contains fluid implementations.
- `blocksandstuff-recipes` contains recipe implementations, although without the actual recipes themselves.

The latest version is always published to our maven repository at [mvn.everbuild.org](https://mvn.everbuild.org/#/public/org/everbuild/blocksandstuff). On the page of the maven repository, you can check the latest version and inclusion information.

After including the module you want to use:

## Common API
> Artifact: `org.everbuild.blocksandstuff:blocksandstuff-common`

There are four parts to the common API:

**Dropped Items**: Implement the `DroppedItemFactory` and register it with
```kt
DroppedItemFactory.current = yourFactory
```
to customize item dropping behavior. The default implementation is `DefaultDroppedItemFactory`.

**Tags**: Some block placement rules require tags that are not present in vanilla. Sometimes, you may want to change these tags. For this, you may override the `blocksandstuff/block/<your-tag>.json` files in your resources. See [some default tags](blocksandstuff-blocks/src/main/resources/blocksandstuff/block) as an example.

**Instance Options**: Want to customize game-rules and similar options? Use the `InstanceOptions` class to set them:
```kt
val options = InstanceOptionsProvider.getForInstance(instance)
options.randomTickSpeed = 10
```

**Block Inventory**: The `BlockInventory` class is a convenient way to manage block inventories. Define a `BlockInventoryArchetype` and attach to a `BlockHandler` using the `BlockInventoryHolder`-interface. See furnaces as [an](blocksandstuff-recipes/src/main/kotlin/org/everbuild/blocksandstuff/recipes/smelting/FurnaceArchetype.kt) [example](blocksandstuff-recipes/src/main/kotlin/org/everbuild/blocksandstuff/recipes/smelting/AbstractSmeltingHandler.kt).

## Blocks API
> Artifact: `org.everbuild.blocksandstuff:blocksandstuff-blocks`

Use the following code-snippet to initialize all implemented vanilla behaviour:
```kt
// Register all default block placement rules
BlockPlacementRuleRegistrations.registerDefault()

// Register all default block behaviors
BlockBehaviorRuleRegistrations.registerDefault()

// Register auto-adding handlers to placed blocks
PlacedHandlerRegistration.registerDefault()

// Pick-Block behaviour
BlockPickup.enable()
```

You can exclude blocks by putting a list of them as parameters to the `registerDefault` methods. You can also only register specific handlers using the `register` methods.

## Fluids API
> Artifact: `org.everbuild.blocksandstuff:blocksandstuff-fluids`

> [!WARNING]
> The fluid API is not stable, very slow, and will be changed in the future. As a discouragement, no docs are provided as of now.

# Recipes API
> Artifact: `org.everbuild.blocksandstuff:blocksandstuff-recipes`

Recipes load from namespaces like [this](testserver/src/main/resources/data/everbuild). You can add your own namespace or copy ours to get started.

Use the `RecipeRegistrations` to register recipes and crafting.
```kt
// Kotlin:
RecipeRegistrations {
    fuelNamespaces += "everbuild"
    recipeNamespaces += "everbuild"
}

// Java:
RecipeRegistrations.builder()
    .fuelNamespace("everbuild")
    .recipeNamespace("everbuild")
    .apply()
```

Use the `addRegistration` and `removeRegistration` methods to modify, which features are loaded. Using the `itemController`, custom items can be loaded from recipes as well. The `stashController` is used to register a hypixel-like stash.

# Versioning

We're using Semantic Versioning for this project:
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

- - -

If you use and/or appreciate this library, we'd appreciate it if you could quickly star it. Also, we'd love to see what you
build with it! Please reach out to us if you have any questions or feedback.