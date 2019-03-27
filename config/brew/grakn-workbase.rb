cask 'grakn-workbase' do
  version '{version}'
  sha256 '{sha256}'

  url "https://github.com/graknlabs/workbase/releases/download/v{version}/grakn-workbase-{version}-mac.dmg"
  name 'Grakn Workbase'
  homepage 'https://grakn.ai/'

  app "Grakn Workbase.app"

end
