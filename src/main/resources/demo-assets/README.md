# Demo Assets

Place demo car images here for use in DEMO_CAR inspection mode.

## Directory Structure

```
demo-assets/
├── clean-car/          # Car with no visible damage
│   ├── front.jpg
│   ├── front-left.jpg
│   ├── left-side.jpg
│   ├── rear-left.jpg
│   ├── rear.jpg
│   ├── rear-right.jpg
│   ├── right-side.jpg
│   └── front-right.jpg
│
├── scratched-car/      # Car with scratches on left door and rear bumper
│   ├── front.jpg
│   ├── left-door-scratch.jpg
│   ├── rear.jpg
│   └── right-side.jpg
│
├── dented-car/         # Car with rear door dent and bumper scratch
│   ├── front.jpg
│   ├── rear-door-dent.jpg
│   ├── bumper-scratch.jpg
│   └── rear.jpg
│
└── difficult-car/      # Edge cases: glare, blur, partial view
    ├── glare.jpg
    ├── blurry.jpg
    ├── partial-view.jpg
    └── damage-visible.jpg
```

## Notes

- Images are not included in the repository.
- Add your own car photos matching the filenames above.
- If an image is missing, the demo frame will send a 1x1 placeholder JPEG and the AI will return no findings.
- Recommended resolution: 1280x720 or similar (the backend will resize automatically).
- JPEG format preferred.
