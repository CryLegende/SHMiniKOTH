<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHMiniKOTH

*Mini King of the Hill au spawn : tenir la plaque rapporte des récompenses à intervalle régulier.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Mini King of the Hill au spawn : tenir la plaque rapporte des récompenses à intervalle régulier.

Utilisé sur : **Opale**.

## Dépendances

- Optionnelles : `PlaceholderAPI`, `SHLogs`

## Commandes

| Commande | Alias | Description | Permission |
|---|---|---|---|
| `/shminikoth` | — | Commande de configuration du plugin SHMiniKOTH (définition de la zone et de la plaque) | `shminikoth.admin` |

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shminikoth.admin` | Permet d'utiliser les commandes administratives de SHMiniKOTH. | `op` |

## Logs

Chaque récompense donnée au roi est écrite dans SHLogs, fichier `recompenses`. Le texte de la ligne se règle avec `rewards.log_format`. Sans SHLogs, la ligne est écrite dans la console.

## Configuration

Fichiers par défaut repris du serveur (secrets remplacés par `CHANGE_ME`). Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHMiniKOTH/
├── <a href="src/main/resources/config.yml">config.yml</a>
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

## Plusieurs serveurs de jeu

La zone du roi est dans le monde du spawn (`zone.pos1` et `zone.pos2`). Quand les joueurs sont répartis sur plusieurs Opale, ce monde n'est hébergé que par un seul d'entre eux, celui du spawn, de l'arène et des événements : un joueur qui y entre depuis un autre Opale est envoyé sur celui-là. Le plugin ne doit donc tourner que sur cet Opale :

- le roi est gardé en mémoire et n'existe que là où se trouve la plaque : un seul roi, un seul `rewards.commands` à l'intervalle choisi, aucune récompense doublée ;
- un roi qui change d'Opale perd la couronne, comme à sa déconnexion ;
- rien n'est partagé, il n'y a ni base ni bus à configurer et SHNetwork n'est pas utilisé ;
- les placeholders `%shminikoth_king%`, `%shminikoth_time%` et `%shminikoth_reward_countdown%` n'existent que sur cet Opale, comme les hologrammes du spawn qui les affichent.

Si la zone était un jour placée dans un monde jouable sur plusieurs Opale, il faudrait un mode réseau : un seul roi et un seul serveur qui paie la récompense.

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
