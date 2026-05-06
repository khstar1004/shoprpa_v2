import { Panel as SplitterPanel } from './Panel'
import { Splitter as RpaSplitter } from './Splitter'
import './style/index.css'

const Splitter = Object.assign(RpaSplitter, {
  Panel: SplitterPanel,
})

export { Splitter, SplitterPanel }
