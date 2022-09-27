# souvenirs-android

Android application for Souvenirs albums

Make photo albums, add text, video, sound and handwritten annotations. Support for photosphere image format in viewer.
Upload your albums on a Nextcloud server and share them with family and friends.

[<img src="https://img.shields.io/f-droid/v/fr.nuage.souvenirs.svg">](https://f-droid.org/packages/fr.nuage.souvenirs/)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/fr.nuage.souvenirs/)

## Yet an other gallery app

Old school paper albums are not just a collection of time ordered photos, they are visuals and texts organised to tell a story, make you remember a moment in the past.

The goal of this app is to mimic a full photo album though image spacial organisation, text and hand drawings, giving you the tools to express and store your memories ("souvenirs" in french). With a touch of more modern items such as video and sound.

Well, at least trying to do it that way with my amateur android programming skills...

## Album structure

Album are composed of several pages where you can include image or text blocs.

Images and text blocs are organised in tiles fully covering the page. You can pan/zoom each images and handwritten annotations over them. You can also manually resize the tiles for a more custom layout.

## Upload your album to a Nextcloud instance

Install the [Souvenir application](https://github.com/zorgluf/souvenirs-nextcloud) on your Nextcloud serveur.

You will need the Nextcloud android client installed and configured on your phone. Then go to the parameters of the Souvenirs app and select your Nextcloud account.

## Permission explained

The application need the following permissions :
* "INTERNET" : used exclusively to upload/download albums from the nextcloud server.
* "WRITE_EXTERNAL_STORAGE" : optional, used only if you want to export your album in a PDF file.
* "RECORD_AUDIO" : optional, only used if you want to insert sound from your microphone.

## Third party libraries

The following libraries are used by this app :
* The media loading library Glide ([https://github.com/bumptech/glide](https://github.com/bumptech/glide))
* The Nextcloud Single Sign On library ([https://github.com/nextcloud/Android-SingleSignOn](https://github.com/nextcloud/Android-SingleSignOn))
* The retrofit http client ([https://square.github.io/retrofit/](https://square.github.io/retrofit/))
* The photo sphere viewer javascript library ([https://photo-sphere-viewer.js.org/](https://photo-sphere-viewer.js.org/))

## Found it useful ?

If you found this project valuable, and want to encourage the author, you can donate at :
[![paypal](https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TRY8KXAN39KJL&source=url)
