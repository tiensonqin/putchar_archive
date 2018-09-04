(ns share.kit.icons
  (:require [share.util :as util]))

(def icons
  {:vote (fn [{:keys [fill width height]}]
           (util/format "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\" class=\"thread-voter__icon ember-view\"><path d=\"M0 15.878 l12-11.878 12 11.878-4 4.122-8-8-8 8-4-4.122z\"></path>
</svg>"
                        fill width height))

   :bookmark (fn [{:keys [fill width height]}]
                  (util/format
                   "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                   fill width height))

   :bookmark_border (fn [{:keys [fill width height]}]
               (util/format
                "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2zm0 15l-5-2.18L7 18V5h10v13z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                fill width height))

   :search (fn [{:keys [width height fill]}]
             (util/format "<svg fill=\"%s\" height=\"%d\" viewBox=\"0 0 24 24\" width=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">
    <path d=\"M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>"
                          fill width height))

   :close (fn [{:keys [width height fill]}]
            (util/format "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\"><path d=\"M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z\"/></svg>"
                         fill width height))

   :favorite (fn [{:keys [width height fill]}]
               (util/format "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\"><path d=\"M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z\"/></svg>"
                            fill width height))

   :favorite_border (fn [{:keys [width height fill]}]
                      (util/format
                       "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\"><path d=\"M16.5 3c-1.74 0-3.41.81-4.5 2.09C10.91 3.81 9.24 3 7.5 3 4.42 3 2 5.42 2 8.5c0 3.78 3.4 6.86 8.55 11.54L12 21.35l1.45-1.32C18.6 15.36 22 12.28 22 8.5 22 5.42 19.58 3 16.5 3zm-4.4 15.55l-.1.1-.1-.1C7.14 14.24 4 11.39 4 8.5 4 6.5 5.5 5 7.5 5c1.54 0 3.04.99 3.57 2.36h1.87C13.46 5.99 14.96 5 16.5 5c2 0 3.5 1.5 3.5 3.5 0 2.89-3.14 5.74-7.9 10.05z\"/></svg>"
                       fill width height))
   :flag (fn [{:keys [width height fill]}]
           (util/format
            "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
<path d=\"M12.36,6l0.4,2H18v6h-3.36l-0.4-2H7V6H12.36 M14,4H5v17h2v-7h5.6l0.4,2h7V6h-5.6L14,4L14,4z\"/>
</svg>
"
            fill width height))
   :reply (fn [{:keys [width height fill]}]
            (util/format
             "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M10 9V5l-7 7 7 7v-4.1c5 0 8.5 1.6 11 5.1-1-5-4-10-11-11z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
             fill width height))
   :expand_less (fn [{:keys [width height fill]}]
                  (util/format
                   "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M12 8l-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                   fill width height))

   :expand_more (fn [{:keys [width height fill]}]
                  (util/format
                   "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                   fill width height))
   :edit (fn [{:keys [class width height fill]}]
           (util/format
            "<svg class=\"%s\" fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
            class fill width height))
   :notifications (fn [{:keys [width height fill]}]
                    (util/format
                     "<svg fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\" xmlns=\"http://www.w3.org/2000/svg\">
    <path d=\"M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z\"/>
</svg>
"
                     fill width height))
   :notifications_none (fn [{:keys [width height fill]}]
                         (util/format
                          "<svg fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\" xmlns=\"http://www.w3.org/2000/svg\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z\"/>
</svg>
"
                          fill width height))
   :notifications_off (fn [{:keys [width height fill]}]
                        (util/format
                         "<svg fill=\"%s\" width=\"%d\" height=\"%d\"viewBox=\"0 0 24 24\" xmlns=\"http://www.w3.org/2000/svg\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M20 18.69L7.84 6.14 5.27 3.49 4 4.76l2.8 2.8v.01c-.52.99-.8 2.16-.8 3.42v5l-2 2v1h13.73l2 2L21 19.72l-1-1.03zM12 22c1.11 0 2-.89 2-2h-4c0 1.11.89 2 2 2zm6-7.32V11c0-3.08-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68c-.15.03-.29.08-.42.12-.1.03-.2.07-.3.11h-.01c-.01 0-.01 0-.02.01-.23.09-.46.2-.68.31 0 0-.01 0-.01.01L18 14.68z\"/>
</svg>
"
                         fill width height))
   :menu (fn [{:keys [width height fill]}]
           (util/format
            "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z\"/>
</svg>
"
            fill width height))
   :mail (fn [{:keys [width height fill]}]
           (util/format
            "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
            fill width height))
   :mail_outline (fn [{:keys [width height fill]}]
                   (util/format
                    "<svg fill=\"%s\" width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">
    <path fill=\"none\" d=\"M0 0h24v24H0z\"/>
    <path d=\"M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 14H4V8l8 5 8-5v10zm-8-7L4 6h16l-8 5z\"/>
</svg>
"
                    fill width height))
   :add_circle_outline (fn [{:keys [width height fill]}]
                         (util/format
                          "<svg fill=\"%s\" width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M13 7h-2v4H7v2h4v4h2v-4h4v-2h-4V7zm-1-5C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z\"/>
</svg>
"
                          fill width height))
   :add (fn [{:keys [width height fill]}]
          (util/format
           "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"
/>
</svg>
"
           fill width height))
   :send (fn [{:keys [width height fill]}]
           (util/format
            "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M2.01 21L23 12 2.01 3 2 10l15 2-15 2z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
            fill width height))
   :photo (fn [{:keys [width height fill]}]
            (util/format
             "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z\"/>
</svg>
"
             fill width height))
   :visibility (fn [{:keys [width height fill]}]
                 (util/format
                  "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z\"/>
</svg>
"
                  fill width height))
   :comments (fn [{:keys [width height fill]}]
               (util/format
                "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M21.99 4c0-1.1-.89-2-1.99-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14l4 4-.01-18zM18 14H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                fill width height))
   :link (fn [{:keys [width height fill]}]
           (util/format
            "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z\"/>
</svg>
"
            fill width height))
   :settings (fn [{:keys [width height fill]}]
               (util/format
                "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
                <path d=\"M19.43,12.98c0.04-0.32,0.07-0.64,0.07-0.98c0-0.34-0.03-0.66-0.07-0.98l2.11-1.65c0.19-0.15,0.24-0.42,0.12-0.64l-2-3.46
                        c-0.09-0.16-0.26-0.25-0.44-0.25c-0.06,0-0.12,0.01-0.17,0.03l-2.49,1c-0.52-0.4-1.08-0.73-1.69-0.98l-0.38-2.65
                        C14.46,2.18,14.25,2,14,2h-4C9.75,2,9.54,2.18,9.51,2.42L9.13,5.07C8.52,5.32,7.96,5.66,7.44,6.05l-2.49-1
                        C4.89,5.03,4.83,5.02,4.77,5.02c-0.17,0-0.34,0.09-0.43,0.25l-2,3.46C2.21,8.95,2.27,9.22,2.46,9.37l2.11,1.65
                        C4.53,11.34,4.5,11.67,4.5,12c0,0.33,0.03,0.66,0.07,0.98l-2.11,1.65c-0.19,0.15-0.24,0.42-0.12,0.64l2,3.46
                        c0.09,0.16,0.26,0.25,0.44,0.25c0.06,0,0.12-0.01,0.17-0.03l2.49-1c0.52,0.4,1.08,0.73,1.69,0.98l0.38,2.65
                        C9.54,21.82,9.75,22,10,22h4c0.25,0,0.46-0.18,0.49-0.42l0.38-2.65c0.61-0.25,1.17-0.59,1.69-0.98l2.49,1
                        c0.06,0.02,0.12,0.03,0.18,0.03c0.17,0,0.34-0.09,0.43-0.25l2-3.46c0.12-0.22,0.07-0.49-0.12-0.64L19.43,12.98z M17.45,11.27
                        c0.04,0.31,0.05,0.52,0.05,0.73c0,0.21-0.02,0.43-0.05,0.73l-0.14,1.13l0.89,0.7l1.08,0.84l-0.7,1.21l-1.27-0.51l-1.04-0.42
                        l-0.9,0.68c-0.43,0.32-0.84,0.56-1.25,0.73l-1.06,0.43l-0.16,1.13L12.7,20H11.3l-0.19-1.35l-0.16-1.13l-1.06-0.43
                        c-0.43-0.18-0.83-0.41-1.23-0.71l-0.91-0.7l-1.06,0.43l-1.27,0.51l-0.7-1.21l1.08-0.84l0.89-0.7l-0.14-1.13
                        C6.52,12.43,6.5,12.2,6.5,12s0.02-0.43,0.05-0.73l0.14-1.13L5.8,9.44L4.72,8.6l0.7-1.21l1.27,0.51l1.04,0.42l0.9-0.68
                        c0.43-0.32,0.84-0.56,1.25-0.73l1.06-0.43l0.16-1.13L11.3,4h1.39l0.19,1.35l0.16,1.13l1.06,0.43c0.43,0.18,0.83,0.41,1.23,0.71
                        l0.91,0.7l1.06-0.43l1.27-0.51l0.7,1.21L18.2,9.44l-0.89,0.7L17.45,11.27z\"/>
                <path d=\"M12,8c-2.21,0-4,1.79-4,4s1.79,4,4,4s4-1.79,4-4S14.21,8,12,8z M12,14c-1.1,0-2-0.9-2-2s0.9-2,2-2s2,0.9,2,2
                        S13.1,14,12,14z\"/>
</svg>
"
                fill width height))

   :info (fn [{:keys [width height fill]}]
           (util/format
            "<svg fill=\"%s\" xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z\"/>
</svg>
"
            fill width height))

   :check_circle (fn [{:keys [width height fill]}]
                   (util/format
                    "<svg fill=\"%s\" width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z\"/>
</svg>
"
                    fill width height))

   :error (fn [{:keys [width height fill]}]
            (util/format
             "<svg fill=\"%s\" width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z\"/>
</svg>
"
             fill width height))
   :warning (fn [{:keys [width height fill]}]
              (util/format
               "<svg fill=\"%s\" width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z\"/>
</svg>
"
               fill width height))

   :translate (fn [{:keys [width height fill]}]
                (util/format
                 "<svg fill=\"%s\" width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12.87 15.07l-2.54-2.51.03-.03c1.74-1.94 2.98-4.17 3.71-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z\"/>
</svg>
"
                 fill width height))

   :twitter (fn [{:keys [width height fill]}]
              (util/format
               "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\"  viewBox=\"0 0 128 128\"><g data-width=\"128\" data-height=\"104\" display=\"inline\" transform=\"translate(0 12)\"><path d=\"M40.255 104c-14.83 0-28.634-4.346-40.255-11.796 2.054.242 4.145.366 6.264.366 12.303 0 23.627-4.197 32.614-11.239-11.491-.212-21.19-7.803-24.531-18.234 1.603.307 3.248.471 4.94.471 2.395 0 4.715-.321 6.919-.921-12.014-2.412-21.065-13.023-21.065-25.744 0-.111 0-.221.002-.33 3.541 1.966 7.59 3.147 11.895 3.284-7.046-4.708-11.683-12.744-11.683-21.853 0-4.811 1.295-9.322 3.555-13.199 12.952 15.884 32.302 26.337 54.128 27.432-.448-1.921-.68-3.925-.68-5.983 0-14.499 11.758-26.254 26.262-26.254 7.553 0 14.378 3.189 19.168 8.291 5.982-1.178 11.602-3.363 16.676-6.371-1.961 6.131-6.125 11.276-11.547 14.525 5.312-.635 10.373-2.046 15.083-4.134-3.521 5.265-7.973 9.889-13.104 13.591.051 1.126.076 2.258.076 3.397 0 34.695-26.414 74.701-74.717 74.701\"></path></g></svg>"
               fill width height))
   :github (fn [{:keys [width height fill]}]
              (util/format
               "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 128 128\"><g data-width=\"32\" data-height=\"32\" display=\"inline\" transform=\"scale(4)\"><path fill-rule=\"evenodd\" clip-rule=\"evenodd\" d=\"M15.999 0c-8.835 0-15.999 7.163-15.999 16.001 0 7.068 4.584 13.065 10.943 15.181.8.147 1.092-.347 1.092-.771 0-.38-.014-1.386-.022-2.721-4.451.967-5.39-2.145-5.39-2.145-.728-1.848-1.776-2.34-1.776-2.34-1.453-.993.11-.973.11-.973 1.606.113 2.451 1.649 2.451 1.649 1.427 2.445 3.745 1.739 4.656 1.329.145-1.034.559-1.739 1.016-2.139-3.553-.404-7.288-1.776-7.288-7.908 0-1.747.623-3.175 1.647-4.293-.164-.405-.713-2.032.157-4.234 0 0 1.343-.43 4.4 1.64 1.276-.355 2.645-.532 4.006-.539 1.359.006 2.728.184 4.006.539 3.055-2.07 4.396-1.64 4.396-1.64.873 2.203.324 3.83.159 4.234 1.025 1.119 1.645 2.547 1.645 4.293 0 6.147-3.741 7.499-7.305 7.895.575.494 1.086 1.47 1.086 2.963 0 2.139-.02 3.865-.02 4.389 0 .428.288.926 1.1.769 6.352-2.12 10.933-8.113 10.933-15.18 0-8.838-7.164-16.001-16.001-16.001z\"></path></g></svg>"
               fill width height))

   :rss (fn [{:keys [width height fill]}]
          (util/format
           "<svg fill=\"%s\" width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 24 24\">
    <path fill=\"none\" d=\"M0 0h24v24H0z\"/>
    <circle cx=\"6.18\" cy=\"17.82\" r=\"2.18\"/>
    <path d=\"M4 4.44v2.83c7.03 0 12.73 5.7 12.73 12.73h2.83c0-8.59-6.97-15.56-15.56-15.56zm0 5.66v2.83c3.9 0 7.07 3.17 7.07 7.07h2.83c0-5.47-4.43-9.9-9.9-9.9z\"/>
</svg>
"
           fill width height))

   :drafts (fn [{:keys [width height fill]}]
             (util/format
              "<svg xmlns=\"http://www.w3.org/2000/svg\"  fill=\"%s\" width=\"%d\" height=\"%d\"  viewBox=\"0 0 24 24\">
    <path d=\"M21.99 8c0-.72-.37-1.35-.94-1.7L12 1 2.95 6.3C2.38 6.65 2 7.28 2 8v10c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2l-.01-10zM12 13L3.74 7.84 12 3l8.26 4.84L12 13z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
              fill width height))

   ;; right-up
   :connect (fn [{:keys [width height fill]}]
              (util/format
               "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 48 48\">
    <path d=\"M0 0h48v48H0z\" fill=\"none\"/>
    <path d=\"M18 10v4h13.17L8 37.17 10.83 40 34 16.83V30h4V10z\"/>
</svg>

"
               fill width height))

   :left-down (fn [{:keys [width height fill]}]
                (util/format
                 "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M20 5.41L18.59 4 7 15.59V9H5v10h10v-2H8.41z\"/>
</svg>
"
                 fill width height))

   :rectangle (fn [{:keys [width height fill]}]
                (util/format
                 "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M19 5H5c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 12H5V7h14v10z\"/>
</svg>
"
                 fill width height))

   :t (fn [{:keys [width height fill]}]
        (util/format
         "<svg class=\"svg\" fill=\"%s\" width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\"><path d=\"M2 5h1V2h5v14H5v1h7v-1H9V2h5v3h1V1H2v4z\"></path></svg>"
         fill width height))

   :file-download (fn [{:keys [width height fill]}]
                    (util/format
                     "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                     fill width height))

   :more (fn [{:keys [width height fill]}]
           (util/format
            "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z\"/>
</svg>
"
            fill width height))

   :more_vert (fn [{:keys [width height fill]}]
           (util/format
            "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z\"/>
</svg>
"
            fill width height))

   :delete (fn [{:keys [width height fill]}]
             (util/format
              "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%s\" height=\"%s\" viewBox=\"0 0 24 24\">
    <path d=\"M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
              fill width height))

   :person_add (fn [{:keys [width height fill]}]
                 (util/format
                  "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%s\" height=\"%s\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z\"/>
</svg>
"
                  fill width height))

   :lock (fn [{:keys [width height fill]}]
           (util/format
            "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z\"/>
</svg>
"
            fill width height))

   :unlock (fn [{:keys [width height fill]}]
           (util/format
            "<svg xmlns=\"http://www.w3.org/2000/svg\"  fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 12H6V10h12v10z\"/>
</svg>
"
            fill width height))

   :star (fn [{:keys [width height fill]}]
           (util/format
            "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
            fill width height))
   :star-border (fn [{:keys [width height fill]}]
                   (util/format
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                    fill width height))
   :back-to-top (fn [{:keys [width height fill]}]
                  (util/format
                   "<svg class=\"back-to-top\" title=\"BACK TO TOP\" viewBox=\"0 0 24 24\" fill=\"%s\" width=\"%d\" height=\"%d\"><path d=\"M16.036 19.59a1 1 0 0 1-.997.995H9.032a.996.996 0 0 1-.997-.996v-7.005H5.03c-1.1 0-1.36-.633-.578-1.416L11.33 4.29a1.003 1.003 0 0 1 1.412 0l6.878 6.88c.782.78.523 1.415-.58 1.415h-3.004v7.005z\"></path></svg>
"
                   fill width height))
   :poll (fn [{:keys [width height fill]}]
                  (util/format
                   "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                   fill width height))

   :format-bold (fn [{:keys [width height fill]}]
                  (util/format
                   "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M15.6 10.79c.97-.67 1.65-1.77 1.65-2.79 0-2.26-1.75-4-4-4H7v14h7.04c2.09 0 3.71-1.7 3.71-3.79 0-1.52-.86-2.82-2.15-3.42zM10 6.5h3c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5h-3v-3zm3.5 9H10v-3h3.5c.83 0 1.5.67 1.5 1.5s-.67 1.5-1.5 1.5z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                   fill width height))
   :format-italic (fn [{:keys [width height fill]}]
                    (util/format
                     "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M10 4v3h2.21l-3.42 8H6v3h8v-3h-2.21l3.42-8H18V4z\"/>
</svg>

"
                     fill width height))

   :format-quote (fn [{:keys [width height fill]}]
                    (util/format
                     "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M6 17h3l2-4V7H5v6h3zm8 0h3l2-4V7h-6v6h3z\"/>
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
</svg>
"
                     fill width height))

   :title (fn [{:keys [width height fill]}]
                   (util/format
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M5 4v3h5.5v12h3V7H19V4z\"/>
    <path fill=\"none\" d=\"M0 0h24v24H0V0z\"/>
</svg>
"
                    fill width height))

   :photo_library (fn [{:keys [width height fill]}]
                    (util/format
                     "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M22 16V4c0-1.1-.9-2-2-2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2zm-11-4l2.03 2.71L16 11l4 5H8l3-4zM2 6v14c0 1.1.9 2 2 2h14v-2H4V6H2z\"/>
</svg>
"
                     fill width height))
   :arrow_upward (fn [{:keys [width height fill]}]
                   (util/format
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path fill=\"none\" d=\"M0 0h24v24H0V0z\"/>
    <path d=\"M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z\"/>
</svg>
"
                    fill width height))
   :thumb_up (fn [{:keys [width height fill]}]
               (util/format
                "<svg fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\"><path d=\"M21.4 8.9c-.6-.7-1.5-1.2-2.5-1.2h-5.8V3.3c0-1.8-1.5-3.3-3.3-3.3-.3 0-.6.2-.7.5l-2.4 9H2.5c-.4 0-.7.3-.7.7v9.5c0 .4.3.7.7.7H18c1.6 0 3-1.2 3.3-2.8l.9-6c.1-1-.1-2-.8-2.7zm-18.2 2h3.3V19H3.2v-8.1zm17.6.4l-.9 6c-.2 1-1 1.7-1.9 1.7H8v-8.7l2.4-8.8c.8.2 1.3.9 1.3 1.8v5.2c0 .4.3.7.7.7h6.5c.5 0 1.1.2 1.4.7.4.4.5.9.5 1.4z\"></path><path d=\"M299 201h24v23.9h-24z\"></path></svg>"
                fill width height))

   :thumb_down (fn [{:keys [width height fill]}]
                   (util/format
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M15 3H6c-.83 0-1.54.5-1.84 1.22l-3.02 7.05c-.09.23-.14.47-.14.73v1.91l.01.01L1 14c0 1.1.9 2 2 2h6.31l-.95 4.57-.03.32c0 .41.17.79.44 1.06L9.83 23l6.59-6.59c.36-.36.58-.86.58-1.41V5c0-1.1-.9-2-2-2zm4 0v12h4V3h-4z\"/>
</svg>
"
                    fill width height))

   :label_outline (fn [{:keys [width height fill]}]
            (util/format
             "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M17.63 5.84C17.27 5.33 16.67 5 16 5L5 5.01C3.9 5.01 3 5.9 3 7v10c0 1.1.9 1.99 2 1.99L16 19c.67 0 1.27-.33 1.63-.84L22 12l-4.37-6.16zM16 17H5V7h11l3.55 5L16 17z\"/>
</svg>
"
             fill width height))

   :share (fn [{:keys [width height fill]}]
            (util/format
             "<svg fill=\"%s\" width=\"%d\" height=\"%d\"><g transform=\"translate(.9 .9)\" stroke=\"#5b5e6a\" stroke-width=\"1.08\" fill=\"none\" fill-rule=\"evenodd\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"13.5\" cy=\"2.7\" r=\"2.7\"></circle><circle cx=\"2.7\" cy=\"8.1\" r=\"2.7\"></circle><circle cx=\"13.5\" cy=\"13.5\" r=\"2.7\"></circle><path d=\"M11.07 3.888L5.13 6.912M5.13 9.288l5.94 3.024\"></path></g></svg>"
             fill width height))

   :logo (fn [{:keys [width height fill]}]
            (util/format
             "<svg fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 72 88\" xmlns=\"http://www.w3.org/2000/svg\">
<rect width=\"70\" height=\"88\" fill=\"%s\" fill-opacity=\"0\"/>
<path d=\"M50.336 57.768C49.9947 59.3467 49.44 60.7973 48.672 62.12C47.904 63.4427 46.9653 64.552 45.856 65.448C43.808 65.448 42.272 64.9787 41.248 64.04C40.224 63.1013 39.3707 61.3307 38.688 58.728L35.04 45.672H34.464C31.8187 52.712 29.152 59.304 26.464 65.448L25.76 65.64L20 63.208L19.808 62.568C24.416 53.0107 28.576 44.1147 32.288 35.88L30.944 30.952C30.4747 29.3307 29.92 28.1787 29.28 27.496C28.64 26.7707 27.744 26.408 26.592 26.408C25.3547 26.408 23.5627 26.8347 21.216 27.688L20.384 26.792C20.4693 22.9093 21.8347 19.9013 24.48 17.768C29.6853 17.768 33.312 21.5653 35.36 29.16L41.632 52.584C42.144 54.4187 42.6773 55.6773 43.232 56.36C43.8293 57.0427 44.7467 57.384 45.984 57.384C46.7947 57.384 48.0107 57.2133 49.632 56.872L50.336 57.768Z\" fill=\"%s\"/>
<path d=\"M71.0607 76.0607C71.6464 75.4749 71.6464 74.5251 71.0607 73.9393L61.5147 64.3934C60.9289 63.8076 59.9792 63.8076 59.3934 64.3934C58.8076 64.9792 58.8076 65.9289 59.3934 66.5147L67.8787 75L59.3934 83.4853C58.8076 84.0711 58.8076 85.0208 59.3934 85.6066C59.9792 86.1924 60.9289 86.1924 61.5147 85.6066L71.0607 76.0607ZM0 76.5L70 76.5V73.5L0 73.5L0 76.5Z\" fill=\"%s\"/>
</svg>
"
             fill width height fill fill fill))

   :ios_back (fn [{:keys [width height fill]}]
            (util/format
             "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
<g id=\"Rounded\">
<path d=\"M16.62,2.99L16.62,2.99c-0.49-0.49-1.28-0.49-1.77,0l-8.31,8.31c-0.39,0.39-0.39,1.02,0,1.41l8.31,8.31
                c0.49,0.49,1.28,0.49,1.77,0h0c0.49-0.49,0.49-1.28,0-1.77L9.38,12l7.25-7.25C17.11,4.27,17.11,3.47,16.62,2.99z\"/>
</g>
</svg>"
             fill width height))
   :home (fn [{:keys [width height fill]}]
                 (util/format
                  "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
<path d=\"M12,5.69l5,4.5V12v6h-2v-4v-2h-2h-2H9v2v4H7v-6v-1.81L12,5.69 M12,3L2,12h3v8h6v-6h2v6h6v-8h3L12,3L12,3z\"/>

</svg>
"
                  fill width height))
   :fullscreen (fn [{:keys [width height fill]}]
                 (util/format
                  "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z\"/>
</svg>
"
                  fill width height))

   :discord (fn [{:keys [width height fill]}]
              (util/format
               "<svg id=\"Layer_1\" xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 245 240\"><path class=\"st0\" d=\"M104.4 103.9c-5.7 0-10.2 5-10.2 11.1s4.6 11.1 10.2 11.1c5.7 0 10.2-5 10.2-11.1.1-6.1-4.5-11.1-10.2-11.1zM140.9 103.9c-5.7 0-10.2 5-10.2 11.1s4.6 11.1 10.2 11.1c5.7 0 10.2-5 10.2-11.1s-4.5-11.1-10.2-11.1z\"/><path class=\"st0\" d=\"M189.5 20h-134C44.2 20 35 29.2 35 40.6v135.2c0 11.4 9.2 20.6 20.5 20.6h113.4l-5.3-18.5 12.8 11.9 12.1 11.2 21.5 19V40.6c0-11.4-9.2-20.6-20.5-20.6zm-38.6 130.6s-3.6-4.3-6.6-8.1c13.1-3.7 18.1-11.9 18.1-11.9-4.1 2.7-8 4.6-11.5 5.9-5 2.1-9.8 3.5-14.5 4.3-9.6 1.8-18.4 1.3-25.9-.1-5.7-1.1-10.6-2.7-14.7-4.3-2.3-.9-4.8-2-7.3-3.4-.3-.2-.6-.3-.9-.5-.2-.1-.3-.2-.4-.3-1.8-1-2.8-1.7-2.8-1.7s4.8 8 17.5 11.8c-3 3.8-6.7 8.3-6.7 8.3-22.1-.7-30.5-15.2-30.5-15.2 0-32.2 14.4-58.3 14.4-58.3 14.4-10.8 28.1-10.5 28.1-10.5l1 1.2c-18 5.2-26.3 13.1-26.3 13.1s2.2-1.2 5.9-2.9c10.7-4.7 19.2-6 22.7-6.3.6-.1 1.1-.2 1.7-.2 6.1-.8 13-1 20.2-.2 9.5 1.1 19.7 3.9 30.1 9.6 0 0-7.9-7.5-24.9-12.7l1.4-1.6s13.7-.3 28.1 10.5c0 0 14.4 26.1 14.4 58.3 0 0-8.5 14.5-30.6 15.2z\"/></svg>"
               fill width height))})
