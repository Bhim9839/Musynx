package com.nitya.musynx.app.service

import java.io.IOException

class PlayableFormatNotFoundException :
    IOException("Playable format not found")

class UnplayableException :
    IOException("Unplayable")

class LoginRequiredException :
    IOException("Login required")

class VideoIdMismatchException :
    IOException("Video id mismatch")