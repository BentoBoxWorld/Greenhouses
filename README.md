# Greenhouses - an add-on for BentoBox

[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/Greenhouses)](https://ci.codemc.org/job/BentoBoxWorld/job/Greenhouses/)[
![Bugs](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_Greenhouses&metric=bugs)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_Greenhouses)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_Greenhouses&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_Greenhouses)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_Greenhouses&metric=ncloc)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_Greenhouses)

## About

Greenhouses is a BentoBox add-on to power-up your island world! It enables players to build their own biome greenhouses complete with weather, friendly mob spawning, unique plant growth, and even block erosion!

Greenhouses are made out of glass and must contain the blocks found in the Biome Recipe to be valid. There is a recipe GUI. Once built, the greenhouse can be used to grow plants with bonemeal, and it may spawn biome-specific mobs. If you include a hopper with water in it, snow will form inside the greenhouse when it rains. If you put bonemeal in the hopper, biome-specific plants will grow. Some blocks can also transform over time due to "erosion".

## Features

* Craft your own self-contained biome greenhouse on an island (or elsewhere if you like)
* Greenhouses can grow plants that cannot normally be grown, like sunflowers
* Friendly mobs can spawn if your greenhouse is well designed - need slimes? Build a swamp greenhouse!
* Blocks change in biomes over time - dirt becomes sand in a desert, dirt becomes clay in a river, for example.
* Greenhouses can run in multiple worlds.
* Easy to use GUI shows greenhouse recipes (e.g. **/is greenhouses**)
* Admins can fully customize biomes and recipes

## How to Build A Greenhouse

This example is for when you are in the BSkyBlock world. For AcidIsland, just use /ai instead of /island.

1. Make glass blocks and build a rectangular set of walls with a flat roof.
2. Put a hopper in the wall or roof.
3. Put a door in the wall so you can get in and out.
4. Type **/island greenhouses** and read the rules for the greenhouse you want.
5. Exit the GUI and place blocks, water, lava, and ice so that you make your desired biome.
6. Type **/island greenhouses** again and click on the biome to make it.
7. Type **/island greenhouses help** to see other options.

### Once made:

* Use bonemeal to grow small plants on grass blocks immediately in the greenhouse.
* Or place bonemeal in the hopper to have the greenhouse sprinkle bonemeal automatically. Come back later to see what grows!
* Place a bucket of water (or more) in the hopper to cause snow to fall in cold biomes. Snow will fall when it rains in the world. Each snowfall empties one bucket of water.
* Friendly biome-specific mobs may spawn in your greenhouse - the usual rules apply (be more than 24 blocks away).

## FAQ

* Can I use stained glass? Yes, you can. It's pretty.
* Can I fill my greenhouse full of water? Yes. That's an ocean.
* Will a squid spawn there? Maybe... okay, yes it will if it's a big enough ocean.
* How do I place a door high up in the wall if the wall is all glass? Place it on a hopper.
* How do I place a door on a hopper? Crouch and then place it.
* Can I use metal doors? Yes.
* Can I use a trap door? Yes.
* Can I grow swamp flowers with this? Yes. Make a swamp biome and use bonemeal.
* How much bonemeal is used to grow plants? One per successful plant.
* How much water do I need to put into the hopper to make it snow? One bucket of water (just the water) is used up every time it rains. This only happens in cold biomes.
* Can I build a Nether greenhouse? Try it and see... (Actually, you may need permission)
* Can I build greenhouses in the Nether? Yes. You can colonize the nether with them.
* What kind of mobs spawn in the biomes? It's what you would expect, wolves in Cold Taiga, horses on plains, etc.


## Required Plugin

This version of Greenhouses is an add-on for BentoBox and will not run stand-alone!

1. BentoBox - make sure you use the latest version!

## Installation and Configuration

1. Download and install BentoBox if you haven't done so already
2. Download the add-on
3. Place into the BentoBox addon's folder
4. Restart your server
5. The addon will make a data folder called greenhouses. Open that folder.
6. Check **config.yml** and edit to be what you want, note the list of world names.
7. Configure the **biomes.yml** if you wish (advanced).
8. Type **/bsbadmin greenhouses reload** in the game to reload the config or restart the server.
9. Done!

To make your first greenhouse, build a glass box and type **/is greenhouses make** to see what kind of greenhouse you get. Type **/is greenhouses** to see the recipes.

## Upgrading

Read the file release notes for changes and instructions on how to upgrade.

## Player Commands

Add these commands to /island, /ai:

* **greenhouses help** - lists these commands
* **greenhouses make**: Tries to make a greenhouse by finding the first valid recipe
* **greenhouses remove**: Removes a greenhouse that you are standing in if you are the owner
* **greenhouses list**: Lists all the recipes available
* **greenhouses recipe**: Display the recipe GUI - clicking on a recipe will try to make a greenhouse

## Admin Commands

Use after the game mode admin command, e.g. /bsb or /acid

* **greenhouses reload** : Reloads config files
* **greenhouses info <player>**: provides info on the player's island greenhouses
* **greenhouses info**: provides info on the greenhouse you are in

## Permissions

Permission to use specific biomes can be added in biomes.yml.

For example, the permission for the Nether biome is **greenhouses.biome.nether** and is set here:

 NETHER:
 
    permission: greenhouses.biome.nether

The permission can be anything you like, e.g., a rank permission, **myserver.VIP**.

### General permissions are:

  greenhouses.player:
  
     description: Gives access to player commands
     default: true
     
  greenhouses.admin:
  
     description: Gives access to admin commands
     default: op
