cask 'grakn-workbase' do
  version '{version}'
  sha256 '{sha256}'

  url "https://github.com/graknlabs/workbase/releases/download/{version}/grakn-workbase-mac-{version}.dmg"
  name 'Grakn Workbase'
  homepage 'https://grakn.ai/'

  app "Grakn Workbase.app"

end
