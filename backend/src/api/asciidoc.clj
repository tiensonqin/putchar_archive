(ns api.asciidoc
  (:import [org.asciidoctor.Asciidoctor]))

;; copy from https://github.com/cjohansen/asciidoclj/blob/master/src/asciidoclj/core.clj

(defn- keywordize-keys [m]
  (zipmap (map keyword (keys m))
          (vals m)))

(defn- map-author [author]
  {:full-name (.getFullName author)
   :last-name (.getLastName author)
   :first-name (.getFirstName author)
   :middle-name (.getMiddleName author)
   :email (.getEmail author)
   :initials (.getInitials author)})

(defn- map-revision-info [revision-info]
  {:date (.getDate revision-info)
   :number (.getNumber revision-info)
   :remark (.getRemark revision-info)})

(defn- map-document-header [header]
  {:document-title (.getDocumentTitle header)
   :page-title (.getPageTitle header)
   :author (map-author (.getAuthor header))
   :authors (map map-author (.getAuthors header))
   :revision-info (map-revision-info (.getRevisionInfo header))
   :attributes (keywordize-keys (.getAttributes header))})

(defn- map-content-part [part]
  {:id (.getId part)
   :level (.getLevel part)
   :context (.getContext part)
   :style (.getStyle part)
   :role (.getRole part)
   :title (.getTitle part)
   :content (.getContent part)
   :parts (map map-content-part (.getParts part))})

(defn- map-structured-document [document]
  {:header (map-document-header (.getHeader document))
   :parts (map map-content-part (.getParts document))})

(defonce doctor (org.asciidoctor.Asciidoctor$Factory/create))
(defn render [str]
  (.convert doctor str (java.util.HashMap.
                        {"safe" (int 1)
                         "attributes" (java.util.HashMap.
                                       {"showTitle" true
                                        "icons" "font"
                                        "source-highlighter" "highlightjs"})})))

(defn parse [str]
  (map-structured-document (.readDocumentStructure doctor str (java.util.HashMap. {}))))
