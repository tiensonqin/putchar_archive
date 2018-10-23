(ns share.kit.icons
  (:require [share.util :as util]))

(def icons
  {:vote (fn [{:keys [fill width height]}]
           (util/format "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\" class=\"thread-voter__icon ember-view\"><path d=\"M0 15.878 l12-11.878 12 11.878-4 4.122-8-8-8 8-4-4.122z\"></path>
</svg>"
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
   :more (fn [{:keys [width height fill]}]
           (util/format
            "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
    <path d=\"M0 0h24v24H0z\" fill=\"none\"/>
    <path d=\"M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z\"/>
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

   :delete (fn [{:keys [width height fill]}]
             (util/format
              "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%s\" height=\"%s\" viewBox=\"0 0 24 24\">
    <path d=\"M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z\"/>
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

   :library_books (fn [{:keys [width height fill]}]
                    (util/format
                     "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\"><path d=\"M4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H9V9h10v2zm-4 4H9v-2h6v2zm4-8H9V5h10v2z\"/></svg>"
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

   :share (fn [{:keys [width height fill]}]
            (util/format
             "<svg fill=\"%s\" width=\"%d\" height=\"%d\"><g transform=\"translate(.9 .9)\" stroke=\"#5b5e6a\" stroke-width=\"1.08\" fill=\"none\" fill-rule=\"evenodd\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><circle cx=\"13.5\" cy=\"2.7\" r=\"2.7\"></circle><circle cx=\"2.7\" cy=\"8.1\" r=\"2.7\"></circle><circle cx=\"13.5\" cy=\"13.5\" r=\"2.7\"></circle><path d=\"M11.07 3.888L5.13 6.912M5.13 9.288l5.94 3.024\"></path></g></svg>"
             fill width height))

   :logo (fn [{:keys [width height fill]}]
           (util/format
            "<svg width=\"%s\" height=\"%s\" viewBox=\"0 0 71 81\" fill=\"none\" xmlns=\"http://www.w3.org/2000/svg\">
<path d=\"M44.7109 0.726562C48.6562 0.726562 52.2305 1.3125 55.4336 2.48438C58.6367 3.65625 61.3516 5.25781 63.5781 7.28906C65.8438 9.32031 67.582 11.7031 68.793 14.4375C70.0039 17.1328 70.6094 20.0039 70.6094 23.0508V58.6758C70.6094 61.7227 70.0039 64.6133 68.793 67.3477C67.582 70.043 65.8438 72.4062 63.5781 74.4375C61.3516 76.4688 58.6367 78.0703 55.4336 79.2422C52.2305 80.4141 48.6562 81 44.7109 81H26.6641C22.7188 81 19.1445 80.4141 15.9414 79.2422C12.7383 78.0703 10.0039 76.4688 7.73828 74.4375C5.51172 72.4062 3.79297 70.043 2.58203 67.3477C1.37109 64.6133 0.765625 61.7227 0.765625 58.6758V23.0508C0.765625 20.0039 1.37109 17.1328 2.58203 14.4375C3.79297 11.7031 5.51172 9.32031 7.73828 7.28906C10.0039 5.25781 12.7383 3.65625 15.9414 2.48438C19.1445 1.3125 22.7188 0.726562 26.6641 0.726562H44.7109ZM55.4922 15.4922L54.6719 10.5703L50.9219 6.82031L44.9453 6H23.0898V54.457H29.1836V37.1719H44.9453L50.9219 36.4688L54.6719 32.7773L55.4922 27.7383V15.4922ZM48.4023 30.375L44.5938 30.9609H29.1836V12.2109H44.5938L48.6367 12.8555L49.2227 16.3125V27.0938L48.4023 30.375Z\" fill=\"%s\"/>
</svg>
"
            width height fill))
   :ios_back (fn [{:keys [width height fill]}]
            (util/format
             "<svg xmlns=\"http://www.w3.org/2000/svg\" fill=\"%s\" width=\"%d\" height=\"%d\" viewBox=\"0 0 24 24\">
<g id=\"Rounded\">
<path d=\"M16.62,2.99L16.62,2.99c-0.49-0.49-1.28-0.49-1.77,0l-8.31,8.31c-0.39,0.39-0.39,1.02,0,1.41l8.31,8.31
                c0.49,0.49,1.28,0.49,1.77,0h0c0.49-0.49,0.49-1.28,0-1.77L9.38,12l7.25-7.25C17.11,4.27,17.11,3.47,16.62,2.99z\"/>
</g>
</svg>"
             fill width height))})
