# Weather background

Home shows current Philadelphia weather in Fahrenheit beneath the date, with spacing between
the city, temperature, and condition. There is no unit toggle or hourly/multi-day forecast panel.

The backdrop follows today's Philadelphia sunrise and sunset, with sunlight or stars, drifting
clouds, fog, rain, or snow depending on the current condition. These are original lightweight
effects, not Apple's assets. The dock and installed apps are unchanged.

## Controls and fallback

- **Launcher settings → Weather background** turns the feature off and restores the existing
  wallpaper/sky behavior. No wallpaper or launcher configuration is deleted.
- **Animations off** or **Reduced motion** keeps the weather scene static.
- Updates happen about every 15 minutes while Home is visible. Failed requests retry after five
  minutes. Valid cached data can be used for up to an hour, only on the same Philadelphia day;
  readings older than 30 minutes are marked cached.
- With no usable reading, Home shows “Weather unavailable” and restores the existing background.
- Animation callbacks stop when Home is paused or its window is hidden. Requests are bounded by
  connection/read timeouts and a response-size limit. No background service is added.

## Data and cost

[Open-Meteo](https://open-meteo.com/en/docs) supplies model-based current conditions, not a live
weather-station feed. The fixed Philadelphia coordinates need no device-location lookup. HTTPS
requests need Android's Internet permission and expose the device's public IP to the provider.
Provider attribution is also available in Launcher settings.

The renderer reuses the existing 480×270 gradient and draws a small native overlay, targeting
20 frames per second when motion is enabled. There are no new runtime dependencies, videos,
downloaded weather images, or forecast UI. Actual smoothness depends on the projector.

`WeatherTest` covers unit conversion, condition mapping, invalid readings, stale/future data,
Philadelphia day boundaries, and sunrise/sunset transitions.
