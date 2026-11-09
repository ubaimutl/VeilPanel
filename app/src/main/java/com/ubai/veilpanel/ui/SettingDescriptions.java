package com.ubai.veilpanel.ui;

/** Short guidance next to the control it explains. */
public final class SettingDescriptions {
 private SettingDescriptions(){}
 public static String forKey(String key){
  switch(key){
   case "split":return "The left side opens Veil. The remaining area opens Android quick settings.";
   case "blur":return "Soften your saved wallpaper. Zero keeps the image sharp.";
   case "glass_opacity":return "Higher values make cards more solid. Contrast protection can add shading over bright images.";
   case "dismiss_handle_width":return "Length of the visible dismiss line.";
   case "dismiss_handle_bottom":return "Move the dismiss line away from the bottom edge.";
   case "dismiss_grab_width":return "Width of the invisible area that catches a dismiss gesture.";
   case "dismiss_grab_height":return "Height of the invisible area around the dismiss line.";
   case "liquid_refraction":return "How strongly the wallpaper bends through the glass.";
   case "liquid_edge":return "Width of the curved lens edge.";
   case "liquid_blur":return "Soften the wallpaper inside glass cards and the glass clock.";
   case "liquid_tint":return "Add a darker tint to the glass. Contrast protection can increase it for readability.";
   case "liquid_highlight":return "Brightness of the light around the glass edge.";
   case "preview_lines":return "Maximum message lines shown before you expand a notification.";
   case "expanded_preview_lines":return "Maximum message lines after expanding a notification.";
   case "description_brightness":return "Brighten message text toward white. Titles keep their own colour.";
   case "clock_size":return "Size of the panel clock, independent of notification text.";
   case "header_clock_weight":return "Higher values make the clock digits heavier.";
   case "header_clock_spacing":return "50 is normal spacing. Lower values bring digits closer together.";
   case "header_clock_top_space":return "Space above the clock. Negative values move it upward.";
   case "header_clock_bottom_space":return "Space below the clock. Negative values pull notifications closer.";
   case "header_date_size":return "Text size for the date and battery line.";
   case "header_date_weight":return "Higher values make the date text heavier.";
   case "clock_glass_frost":return "Frost inside the liquid glass clock digits.";
   case "clock_glass_highlight":return "Brightness of the clock's glass edge.";
   case "clock_glass_refraction":return "Wallpaper distortion inside the clock digits.";
   case "clock_glass_bevel":return "Width of the sculpted edge around each digit.";
   case "card_width":return "Card width as a percentage of the available panel width.";
   case "card_height":return "Minimum height of a notification card. Longer content can make it taller.";
   case "card_padding":return "Space between a card's edges and its contents.";
   case "card_gap":return "Space between notification cards.";
   case "card_radius":return "Higher values give cards rounder corners.";
   case "side_margin":return "Space between notifications and the sides of the screen.";
   case "icon_gap":return "Space between the app icon and notification text.";
   case "header_gap":return "Space between the header and the notification list.";
   case "icon_size":return "Size of the app icon on each notification.";
   case "icon_corner":return "Round the corners of notification app icons.";
   case "headline_size":return "Size of notification titles.";
   case "body_size":return "Size of message and description text.";
   case "time_size":return "Size of notification timestamps.";
   case "font_weight":return "Higher values make notification titles heavier.";
   case "control_icon_size":return "Size of expand, close and other control icons.";
   case "control_surface_size":return "Size of the optional border around controls.";
   case "motion_damping":return "Tune the return of the list when elastic bounce is enabled.";
   default:return null;
  }
 }
}
