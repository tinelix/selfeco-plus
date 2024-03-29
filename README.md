# SelfEco Plus
_Based on [original SelfEco project](https://github.com/monobogdan/selfeco)._ 
##### Для русскоязычных пользователей смотрите README.md на [русском языке](https://github.com/tinelix/selfeco-plus/blob/main/README.RU.md).

## Backstory
**These are the pages of the "your own ecosystem" project.**

Some time ago, I decided to take my favorite QWERTY Android smartphones off the shelves
and try to breathe a second life into them - after all, the form factor of the devices
is very cool, but other than dialers and checking mail, they weren’t really good for
anything in their current state. Therefore, I decided to create for them the most necessary,
imo, application clients - VK (music, messenger), YouTube and parcel tracking.

All projects work via a relay server HTTPS -> HTTP and parse data manually. In fact,
these are just “faces” to the datasets that come from the server.

## MiniVK
### Compact VK unofficial messenger

A very primitive messenger, which, nevertheless, may be useful to someone. There are dialogs with
notifications, there is a feed, there are walls, there are user profiles.
In addition, the application has a full-fledged music section with search, downloading tracks<sup>\*</sup>
and background listening<sup>\*</sup>.

###### \* DISCLAIMER: These actions violate the [VK Terms of Service](https://vk.com/terms). Claims to the author of the original project.

## Blummer
### Compact YouTube mobile front-end

Works via [Invidous](https://invidious.io/) API. Since YouTube no longer knows how to serve videos on old mobile phones
can load in a stream (working with the network puts a good load on the CPU), the client downloads
them in a convenient way format and allows you to watch them offline or immediately. In general, with
a fast Internet, loading.
A 30 minute video takes about a minute :)

## Tracking
Not yet ready :(

# How to Download?
Go to [Releases](https://github.com/tinelix/selfeco-plus/releases) and download latest version.

# How to Report Bugs?
Go to [Issues](https://github.com/tinelix/selfeco-plus/issues).
