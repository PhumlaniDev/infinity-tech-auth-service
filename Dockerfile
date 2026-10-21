notify:
    needs: [build, sast, sonar, docker, sca]
    if: always()
    uses: PhumlaniDev/reusable-workflows/.github/workflows/discord-notify.yml@main
    with:
      # Colour is derived from this status inside the reusable workflow.
      status: >-
        ${{ contains(needs.*.result, 'failure') && 'failure'
        || contains(needs.*.result, 'cancelled') && 'cancelled'
        || 'success' }}
      title: "🚀 auth-service CI"
      description: |
        Build (incl. Checkstyle): `${{ needs.build.result }}`
        SAST: `${{ needs.sast.result }}`
        Sonar: `${{ needs.sonar.result }}`
        Docker: `${{ needs.docker.result }}`
        SCA: `${{ needs.sca.result }}`
        Branch: `${{ github.ref_name }}`
        [View commit](https://github.com/${{ github.repository }}/commit/${{ github.sha }})
    secrets:
      DISCORD_WEBHOOK: ${{ secrets.DISCORD_WEBHOOK }}
