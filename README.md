# Truth or Dare Bot
A simple discord bot that allows you to play truth or dare with your friends.

## How to self-host
1. Download Java 21 or higher from [here](https://adoptium.net/temurin/releases/).
2. Download the latest release (.jar) of the bot from [here](https://github.com/DaRealTurtyWurty/TruthOrDareBot/releases).
3. Go to the [discord developer portal](https://discord.com/developers/applications) and create a new application.
4. Go to the bot tab and create a new bot.
5. Copy the token and save it somewhere safe.
6. Invite the bot to your server using the following link: `https://discord.com/oauth2/authorize?client_id=YOUR_CLIENT_ID&scope=bot&permissions=8`.  
   Replace `YOUR_CLIENT_ID` with the client id of your bot (or alternatively use the URL generator in the discord developer portal).
7. Create a new file in the location of your jar file called `.env` and add the following line: `BOT_TOKEN=YOUR_BOT_TOKEN`. 
   Replace `YOUR_BOT_TOKEN` with the token you copied earlier.
8. Run the jar file using the following command: `java -jar TruthOrDareBot-1.0.jar`.
9. (Optional) Instead of having the `.env` file in the same directory as the jar file, you can also run the jar with the `-env` argument and specify the path to the `.env` file: `java -jar TruthOrDareBot-1.0.0.jar -env /path/to/.env`
10. The bot should now be running and you can use the `/truth` and `/dare` commands in your server.

Note: When running the bot in servers, it will create a `data` folder in the same directory as the jar file. This folder will contain a JSON file for each server that the bot is in. These files store the config and packs for each server.

## Commands
- `/truth` - Get a random truth question.
- `/dare` - Get a random dare.
- `/random` - Get a random truth or dare.
-`/help` - Get a list of all available commands.

### Server Only Commands
- `/pack list` - Get a list of all available packs.
- `/pack add <name> [description]` - Add a new pack to the bot.
- `/pack remove <name>` - Remove a pack from the bot.
- `/pack view <type> <name>` - View the truths or dares in a specific pack.
- `/pack use <name>` - Set this server to use a specific pack.
- `/edit-pack description <name> <description>` - Change the description of a pack.
- `/edit-pack add-truth <name> <truth>` - Add a truth to a pack.
- `/edit-pack add-dare <name> <dare>` - Add a dare to a pack.
- `/edit-pack remove-truth <name> <index>` - Remove a truth from a pack.
- `/edit-pack remove-dare <name> <index>` - Remove a dare from a pack.
- `/add-config blacklist-channel <channel>` - Add a channel to the blacklist.
- `/add-config whitelist-channel <channel>` - Add a channel to the whitelist.
- `/add-config blacklist-role <role>` - Add a role to the blacklist.
- `/add-config whitelist-role <role>` - Add a role to the whitelist.
- `/remove-config blacklist-channel <channel>` - Remove a channel from the blacklist.
- `/remove-config whitelist-channel <channel>` - Remove a channel from the whitelist.
- `/remove-config blacklist-role <role>` - Remove a role from the blacklist.
- `/remove-config whitelist-role <role>` - Remove a role from the whitelist.
- `/reset-config [type: all|channel_whitelist|channel_blacklist|role_whitelist|role_blacklist]` - Reset the config for the server.
- `/get-config` - Get the current config for the server.

## Contributing
If you would like to contribute to the project, feel free to fork the repository and submit a pull request.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
