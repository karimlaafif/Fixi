# Illustrations de Fixi

Les illustrations ont été générées avec **Codex CLI** (outil de génération d'images d'OpenAI), en donnant les maquettes de `documents/ui/` comme références de style, puis détourées (fond blanc retiré) et redimensionnées.

| Fichier (`src/main/resources/images/`) | Taille | Utilisé dans |
|---|---|---|
| `logo-mark.png` | 256 × 256 | Panneau de connexion / inscription, icône de la fenêtre |
| `logo-white.png` | 256 × 256 | Menu latéral violet (tracé blanc seul, dérivé de `logo-mark.png`) |
| `mascot.png` | 857 × 900 | Connexion, inscription, bannière d'accueil, nouvelle demande, tableau de bord artisan |
| `trades/<metier>.png` | 256 max | Tuiles « Métiers », lignes de demandes, en-tête du détail d'une demande |

## Ajouter l'illustration d'un nouveau métier

1. Nom du fichier = nom français du métier, **sans accents, en minuscules, espaces remplacés par des tirets** :
   « Pose de carrelage » → `trades/pose-de-carrelage.png`.
2. PNG à fond transparent, environ 256 px de côté.
3. Sans fichier, l'application affiche automatiquement une icône simple : rien ne casse.

## Prompts utilisés (pour régénérer dans le même style)

Commande :

```bash
/Applications/ChatGPT.app/Contents/Resources/codex-cli/bin/codex exec --skip-git-repo-check \
  -s workspace-write -C <dossier-de-sortie> -i "documents/ui/fixi home" "<prompt>"
```

Style commun ajouté à chaque prompt :

> Flat vector illustration with soft shading, purple palette: deep purple #5B25C9, violet #7C4DFF, lavender #E9E0FC. Plain pure white (#FFFFFF) background, no floor, no shadow, no text.

- **Mascotte** : friendly young handyman smiling, purple cap with small white gear logo, white t-shirt, purple overalls, holding a grey wrench raised in his right hand, other hand in pocket, standing in front of a simple purple house with chimney and a large lavender gear on its facade. Full figure from head to knees, centered.
- **Métier** (un par image, 1024 × 1024) : *single object centered, colorful semi-flat 3D-ish icon, fills 70 % of the canvas* — par exemple « blue water faucet with a falling water drop ».
- **Logo** : rounded-square app icon, deep purple background, simple white line-art house outline containing a wrench, flat vector, no text.

Le fond blanc est ensuite retiré par remplissage depuis les bords (les zones blanches à l'intérieur du dessin, comme le t-shirt, sont conservées).
