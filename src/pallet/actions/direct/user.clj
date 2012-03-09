(ns pallet.actions.direct.user
  "User management action."
  (:use
   [pallet.action :only [implement-action]]
   [pallet.actions :only [user group]]
   [pallet.script :only [defscript]])
  (:require
   [pallet.script.lib :as lib]
   [pallet.stevedore :as stevedore]
   [clojure.string :as string]))


(def
  ^{:doc "Map for looking up shell path based on keyword." :private true}
  shell-names
  {:bash "/bin/bash" :csh "/bin/csh" :ksh "/bin/ksh" :rsh "/bin/rsh"
   :sh "/bin/sh" :tcsh "/bin/tcsh" :zsh "/bin/zsh" :false "/bin/false"})

(defn user*
  "Require a user"
  [session username & {:keys [action shell base-dir home system create-home
                              password shell comment groups remove force append]
                       :or {action :manage}
                       :as options}]
  (let [opts (merge options {:shell (get shell-names shell shell)})]
    (case action
      :create
      (stevedore/script
       (if-not (~lib/user-exists? ~username)
         (~lib/create-user
          ~username ~(select-keys opts [:base-dir :home :system :comment
                                        :create-home :password :shell
                                        :group :groups]))))
      :manage
      (stevedore/script
       (if (~lib/user-exists? ~username)
         (~lib/modify-user
          ~username ~(select-keys
                      opts [:home :shell :comment :group :groups :password
                            :append]))
         (~lib/create-user
          ~username ~(select-keys opts [:base-dir :home :system :comment
                                        :create-home :password :shell
                                        :group :groups]))))
      :lock
      (stevedore/script
       (if (~lib/user-exists? ~username)
         (~lib/lock-user ~username)))
      :unlock
      (stevedore/script
       (if (~lib/user-exists? ~username)
         (~lib/unlock-user ~username)))
      :remove
      (stevedore/script
       (if (~lib/user-exists? ~username)
         (~lib/remove-user ~username ~(select-keys opts [:remove :force]))))
      (throw (IllegalArgumentException.
              (str action " is not a valid action for user action"))))))


(implement-action user :direct
  {:action-type :script :location :target}
  [session & user-args]
  [[{:language :bash}
    (string/join \newline (map #(apply user* session %) user-args))]
   session])


(implement-action group :direct
  {:action-type :script :location :target}
  [session groupname & {:keys [action system gid password]
                        :or {action :manage}
                        :as options}]
  [[{:language :bash}
    (case action
      :create
      (stevedore/script
       (if-not (~lib/group-exists? ~groupname)
         (~lib/create-group
          ~groupname ~(select-keys options [:system :gid :password]))))
      :manage
      (stevedore/script
       (if (~lib/group-exists? ~groupname)
         (~lib/modify-group
          ~groupname ~(select-keys options [:gid :password]))
         (~lib/create-group
          ~groupname ~(select-keys options [:system :gid :password]))))
      :remove
      (stevedore/script
       (if (~lib/group-exists? ~groupname)
         (~lib/remove-group ~groupname {})))
      (throw (IllegalArgumentException.
              (str action " is not a valid action for group action"))))]
   session])
