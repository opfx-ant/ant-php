<?php
//class_alias('SebastianBergmann\CodeCoverage\CodeCoverage', 'CodeCoverage');
if (class_exists('PHPUnit_Util_Printer')) {
	class_alias('PHPUnit_TextUI_ResultPrinter', 'ResultPrinter');
 	class_alias('PHPUnit_Framework_TestResult', 'TestResult');
} else {
	class_alias('PHPUnit\TextUI\ResultPrinter', 'ResultPrinter');
	class_alias('PHPUnit\Framework\TestResult', 'TestResult');
	class_alias('PHPUnit\Framework\TestSuite', 'TestSuite');
	class_alias('PHPUnit\Framework\Test', 'Test');
	class_alias('PHPUnit\Framework\SelfDescribing', 'SelfDescribing');
	class_alias('PHPUnit\Framework\TestFailure', 'TestFailure');
	class_alias('PHPUnit\Framework\ExceptionWrapper', 'ExceptionWrapper');
	class_alias('PHPUnit\Framework\AssertionFailedError', 'AssertionFailedError');
	class_alias('PHPUnit\Framework\Warning', 'Warning');
	class_alias('PHPUnit\Util\Filter', 'Filter');
	class_alias('PHPUnit\Util\Xml', 'Xml');
}

class PHPUnitResultPrinter extends ResultPrinter
{
	protected $document;
	/**
	 * @var DOMElement
	 */
	protected $root;
	private $progressStarted;
	protected $testSuiteLevel = 0;
	
	/**
	 * @var DOMElement[]
	 */
	protected $testSuites = [];
	
	/**
	 * @var int[]
	 */
	protected $testSuiteTests = [0];
	
	/**
	 * @var int[]
	 */
	protected $testSuiteAssertions = [0];
	
	/**
	 * @var int[]
	 */
	protected $testSuiteErrors = [0];
	
	/**
	 * @var int[]
	 */
	protected $testSuiteFailures = [0];
	
	/**
	 * @var int[]
	 */
	protected $testSuiteSkipped = [0];
	
	/**
	 * @var int[]
	 */
	protected $testSuiteTimes = [0];
	
	/**
	 * @var ?DOMElement
	 */
	protected $currentTestCase;
	
	public function __construct($out = null, $verbose = false, $colors = self::COLOR_DEFAULT, $debug = false, $numberOfColumns = 80, $reverse = false)	{
		parent::__construct($out, $verbose, $colors, $debug, $numberOfColumns, $reverse);
		$this->progressStarted = false;
		$this->document = new DOMDocument('1.0', 'UTF-8');
		$this->root = $this->document->createElement('testsuites');
		$this->document->appendChild($this->root);
	}
	
	public function addError(Test $test, Exception $e, $time)	{
		parent::addError($test,$e,$time);
		$this->doAddFault($test, $e, $time, 'error');
		$this->testSuiteErrors[$this->testSuiteLevel]++;
	}
	
	public function addFailure(Test $test, AssertionFailedError $e, $time)
	{
		parent::addFailure($test, $e, $time);
		$this->doAddFault($test, $e, $time, 'failure');
		$this->testSuiteFailures[$this->testSuiteLevel]++;
	}
	
	public function addIncompleteTest(Test $test, Exception $e, $time)
	{
		parent::addIncompleteTest($test, $e,$time);
		$this->doAddSkipped($test);
	}
	
	public function addRiskyTest(Test $test, Exception $e, $time)
	{
		parent::addRiskyTest($test, $e, time);
		if (!$this->reportUselessTests || $this->currentTestCase === null) {
			return;
		}
		
		$error = $this->document->createElement(
				'error',
				Xml::prepareString(
						"Risky Test\n" .
						Filter::getFilteredStacktrace($e)
						)
				);
		
		$error->setAttribute('type', get_class($e));
		
		$this->currentTestCase->appendChild($error);
		
		$this->testSuiteErrors[$this->testSuiteLevel]++;
	}
	
	public function addSkippedTest(Test $test, Exception $e, $time)
	{
		parent::addSkippedTest($test, $e, $time);
		$this->doAddSkipped($test);
	}
	
	
	public function addWarning(Test $test, Warning $e, $time)
	{
		parent::addWarning($test, $e, $time);
		$this->doAddFault($test, $e, $time, 'warning');
		$this->testSuiteFailures[$this->testSuiteLevel]++;
	}
	
	public function startTestSuite(TestSuite $suite)
	{
		parent::startTestSuite($suite);
		
		$testSuite = $this->document->createElement('testsuite');
		$testSuite->setAttribute('name', $suite->getName());
		
		if (class_exists($suite->getName(), false)) {
			try {
				$class = new ReflectionClass($suite->getName());
				
				$testSuite->setAttribute('file', $class->getFileName());
			} catch (ReflectionException $e) {
			}
		}
		
		if ($this->testSuiteLevel > 0) {
			$this->testSuites[$this->testSuiteLevel]->appendChild($testSuite);
		} else {
			$this->root->appendChild($testSuite);
		}
// 		if(isset($this->testSuites[$this->testSuiteLevel])) {
// 			$this->testSuites[$this->testSuiteLevel]->appendChild($testSuite);
// 		}
		
		$this->testSuiteLevel++;
		$this->testSuites[$this->testSuiteLevel]          = $testSuite;
		$this->testSuiteTests[$this->testSuiteLevel]      = 0;
		$this->testSuiteAssertions[$this->testSuiteLevel] = 0;
		$this->testSuiteErrors[$this->testSuiteLevel]     = 0;
		$this->testSuiteFailures[$this->testSuiteLevel]   = 0;
		$this->testSuiteSkipped[$this->testSuiteLevel]    = 0;
		$this->testSuiteTimes[$this->testSuiteLevel]      = 0;
	}
	
	public function endTestSuite(TestSuite $suite)
	{
		parent::endTestSuite($suite);
		$this->testSuites[$this->testSuiteLevel]->setAttribute(
				'tests',
				$this->testSuiteTests[$this->testSuiteLevel]
				);
		
		$this->testSuites[$this->testSuiteLevel]->setAttribute(
				'assertions',
				$this->testSuiteAssertions[$this->testSuiteLevel]
				);
		
		$this->testSuites[$this->testSuiteLevel]->setAttribute(
				'errors',
				$this->testSuiteErrors[$this->testSuiteLevel]
				);
		
		$this->testSuites[$this->testSuiteLevel]->setAttribute(
				'failures',
				$this->testSuiteFailures[$this->testSuiteLevel]
				);
		
		$this->testSuites[$this->testSuiteLevel]->setAttribute(
				'skipped',
				$this->testSuiteSkipped[$this->testSuiteLevel]
				);
		
		$this->testSuites[$this->testSuiteLevel]->setAttribute(
				'time',
				sprintf('%F', $this->testSuiteTimes[$this->testSuiteLevel])
				);
		
		if ($this->testSuiteLevel > 1) {
			$this->testSuiteTests[$this->testSuiteLevel - 1] += $this->testSuiteTests[$this->testSuiteLevel];
			$this->testSuiteAssertions[$this->testSuiteLevel - 1] += $this->testSuiteAssertions[$this->testSuiteLevel];
			$this->testSuiteErrors[$this->testSuiteLevel - 1] += $this->testSuiteErrors[$this->testSuiteLevel];
			$this->testSuiteFailures[$this->testSuiteLevel - 1] += $this->testSuiteFailures[$this->testSuiteLevel];
			$this->testSuiteSkipped[$this->testSuiteLevel - 1] += $this->testSuiteSkipped[$this->testSuiteLevel];
			$this->testSuiteTimes[$this->testSuiteLevel - 1] += $this->testSuiteTimes[$this->testSuiteLevel];
		}
		
		$this->testSuiteLevel--;
	}
	
	
	public function startTest(Test $test)
	{
		parent::startTest($test);
		$testCase = $this->document->createElement('testcase');
		$testCase->setAttribute('name', $test->getName());
		
		if ($test instanceof TestCase) {
			$class      = new ReflectionClass($test);
			$methodName = $test->getName(!$test->usesDataProvider());
			
			if ($class->hasMethod($methodName)) {
				$method = $class->getMethod($methodName);
				
				$testCase->setAttribute('class', $class->getName());
				$testCase->setAttribute('classname', str_replace('\\', '.', $class->getName()));
				$testCase->setAttribute('file', $class->getFileName());
				$testCase->setAttribute('line', $method->getStartLine());
			}
		}
		
		$this->currentTestCase = $testCase;
	}
	/**
	 * A test ended.
	 *
	 * @param Test  $test
	 * @param float $time
	 */
	public function endTest(Test $test, $time)	{
		parent::endTest($test,$time);
		if ($test instanceof TestCase) {
			$numAssertions = $test->getNumAssertions();
			$this->testSuiteAssertions[$this->testSuiteLevel] += $numAssertions;
			
			$this->currentTestCase->setAttribute(
					'assertions',
					$numAssertions
					);
		}
		
		$this->currentTestCase->setAttribute(
				'time',
				\sprintf('%F', $time)
				);
		
		$this->testSuites[$this->testSuiteLevel]->appendChild(
				$this->currentTestCase
				);
		
		$this->testSuiteTests[$this->testSuiteLevel]++;
		$this->testSuiteTimes[$this->testSuiteLevel] += $time;
		
		if (method_exists($test, 'hasOutput') && $test->hasOutput()) {
			$systemOut = $this->document->createElement('system-out');
			
			$systemOut->appendChild(
					$this->document->createTextNode($test->getActualOutput())
					);
			
			$this->currentTestCase->appendChild($systemOut);
		}
		
		$this->currentTestCase = null;
	}
	
	
	
	public function printResult(TestResult $result) {
 		$cc=$result->getCodeCoverage();
 		if($cc!==null) {
			$s = serialize($cc);
 			file_put_contents('O:\work\opfx\proj\ant\master\ant-php\build\work\money\build\report\1.txt', $s);
 			$cc->setProcessUncoveredFilesFromWhitelist(false);
 			$cc->setAddUncoveredFilesFromWhitelist(false);
 			$cc->clear(); 			
 		}
 		parent::printResult($result);
	}
	
	protected function writeProgress($progress)
	{
		if(!$this->progressStarted) {
			fwrite(STDOUT, '+');
			$this->progressStarted = true;
		}
		fwrite(STDOUT,$progress);
		$this->column++;
		$this->numTestsRun++;	
	}
	
	protected function printHeader()
	{
		$millies = round((microtime(true) - PHP_Timer::$requestTime)*1000);
		$mbytes =  memory_get_peak_usage(true) / 1048576;
		fwrite(STDOUT,"#T:$millies;M:$mbytes#");
	}
	
	protected function printDefects(array $defects, $type) {		
		if(empty($defects)) {
			return;
		}
		if($this->reverse){
			$defects = array_reverse($defects);
		}
		foreach($defects as $defect) {
			$trace = [];
			$e = $defect->thrownException();
			
			
			$trace[] = str_replace(["\r","\n"],['','#'],(string)$e);
			
			while ($e = $e->getPrevious()) {
				$trace[]=" Caused by " .str_replace(["\r","\n"],['','#'],(string)$e);
			}
			$trace = implode('#',$trace);
			$defect = sprintf("\n%s|%s#%s", $type,$defect->getTestName(),$trace);
			fwrite(STDERR,$defect);
		}
	}
	
	

	protected function printFooter(TestResult $result)
	{
		if (count($result) === 0) {
			$this->writeWithColor(
					'fg-black, bg-yellow',
					'No tests executed!'
					);
			
			return;
		}
		$xml = $this->document->saveXML();
		
		if ($result->wasSuccessful() &&
				$result->allHarmless() &&
				$result->allCompletelyImplemented() &&
				$result->noneSkipped()) {
					$this->writeWithColor(
							'fg-black, bg-green',
							sprintf(
									'OK (%d test%s, %d assertion%s)',
									count($result),
									(count($result) == 1) ? '' : 's',
									$this->numAssertions,
									($this->numAssertions == 1) ? '' : 's'
									)
							);
				} else {
					if ($result->wasSuccessful()) {
						$color = 'fg-black, bg-yellow';
						
						if ($this->verbose) {
							$this->write("\n");
						}
						
						$this->writeWithColor(
								$color,
								'OK, but incomplete, skipped, or risky tests!'
								);
					} else {
						$this->write("\n");
						
// 						if ($result->errorCount()) {
// 							$color = 'fg-white, bg-red';
							
// 							$this->writeWithColor(
// 									$color,
// 									'ERRORS!'
// 									);
// 						} elseif ($result->failureCount()) {
// 							$color = 'fg-white, bg-red';
							
// 							$this->writeWithColor(
// 									$color,
// 									'FAILURES!'
// 									);
// 						} elseif ($result->warningCount()) {
// 							$color = 'fg-black, bg-yellow';
							
// 							$this->writeWithColor(
// 									$color,
// 									'WARNINGS!'
// 									);
// 						}
					}
					
				
					
					$count = count($result);
					fwrite(STDOUT,"T:$count;");
					$count = $this->numAssertions;
					fwrite(STDOUT,"A:$count;");
					$count = $result->errorCount();
					fwrite(STDOUT,"E:$count;");
					$count = $result->failureCount();
					fwrite(STDOUT,"F:$count;");
					$count = $result->warningCount();
					fwrite(STDOUT,"W:$count;");
					$count = $result->skippedCount();
					fwrite(STDOUT,"S:$count;");
					$count = $result->notImplementedCount();
					fwrite(STDOUT,"I:$count;");
					$count = $result->riskyCount();
					fwrite(STDOUT,"R:$count;");
					
				}
	}
	
	/**
	 * Method which generalizes addError() and addFailure()
	 *
	 * @param Test       $test
	 * @param \Exception $e
	 * @param float      $time
	 * @param string     $type
	 */
	private function doAddFault(Test $test, Exception $e, $time, $type)	{
		if ($this->currentTestCase === null) {
			return;
		}
		
		if ($test instanceof SelfDescribing) {
			$buffer = $test->toString() . "\n";
		} else {
			$buffer = '';
		}
		
		$buffer .= TestFailure::exceptionToString($e) . "\n" .
				Filter::getFilteredStacktrace($e);
				
				$fault = $this->document->createElement(
						$type,
						Xml::prepareString($buffer)
						);
				
				if ($e instanceof ExceptionWrapper) {
					$fault->setAttribute('type', $e->getClassName());
				} else {
					$fault->setAttribute('type', \get_class($e));
				}
				
				$this->currentTestCase->appendChild($fault);
	}
	
	private function doAddSkipped(Test $test)	{
		if ($this->currentTestCase === null) {
			return;
		}
		
		$skipped = $this->document->createElement('skipped');
		$this->currentTestCase->appendChild($skipped);
		
		$this->testSuiteSkipped[$this->testSuiteLevel]++;
	}
	
}

class_alias('PHPUnitResultPrinter', 'PHPUnitResultPrinter@version.script@');

